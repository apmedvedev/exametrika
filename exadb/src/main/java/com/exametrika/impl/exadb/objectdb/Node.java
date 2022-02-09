/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import java.util.Map;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.ComplexField;
import com.exametrika.impl.exadb.objectdb.fields.SimpleField;
import com.exametrika.impl.exadb.objectdb.fields.VersionField;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.NodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFullTextField;


/**
 * The {@link Node} is an abstract node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class Node implements INode {
    public static final int HEADER_SIZE = 28;
    protected static final byte FLAG_NEW = 0x1;
    protected static final byte FLAG_MODIFIED = 0x2;
    protected static final byte FLAG_STALE = 0x4;
    protected static final byte FLAG_NONCACHED = 0x8;
    protected final NodeSpace space;
    protected final long nodeBlockIndex;
    protected INodeSchema schema;
    protected IField[] fields;
    protected Element<Node> element = new Element<Node>(this);
    protected Element<Node> writeElement;
    protected Element<Node> committedElement;
    protected IRawPage headerPage;
    protected int refreshIndex;
    protected INodeObject object;
    protected int lastAccessTime;
    protected byte flags;
    protected int cacheSize;

    public Node(NodeSpace space, INodeSchema schema, long nodeBlockIndex, IRawPage headerPage, int nodeCacheSize) {
        Assert.notNull(space);
        Assert.notNull(schema);
        Assert.isTrue(nodeBlockIndex != 0);
        Assert.notNull(headerPage);

        this.space = space;
        this.schema = schema;
        this.nodeBlockIndex = nodeBlockIndex;
        this.headerPage = headerPage;
        cacheSize = schema.getConfiguration().getCacheSize() + nodeCacheSize;
        refreshIndex = space.getNodeCache().getRefreshIndex();
    }

    public int getRefreshIndex() {
        return refreshIndex;
    }

    public IRawPage getHeaderPage() {
        return headerPage;
    }

    @Override
    public NodeSpace getSpace() {
        return space;
    }

    public final long getNodeBlockIndex() {
        return nodeBlockIndex;
    }

    public final int getHeaderOffset() {
        return Constants.pageOffsetByBlockIndex(nodeBlockIndex);
    }

    public final Element<Node> getElement() {
        return element;
    }

    public final Element<Node> getWriteElement() {
        if (writeElement == null)
            writeElement = new Element<Node>(this);
        return writeElement;
    }

    public final boolean isUncommitted() {
        return committedElement != null;
    }

    public final Element<Node> getCommittedElement() {
        if (committedElement == null)
            committedElement = new Element<Node>(this);

        return committedElement;
    }

    public void setStale() {
        flags |= FLAG_STALE;
        element = null;
        writeElement = null;
        committedElement = null;
        fields = null;
        if (object instanceof NodeObject)
            ((NodeObject) object).setUnloaded();
        object = null;
        headerPage = null;
        schema = null;
    }

    public final int getLastAccessTime() {
        return lastAccessTime;
    }

    public final void setLastAccessTime(int time) {
        lastAccessTime = time;
    }

    public final IField getFieldInstance(int index) {
        if (fields != null && fields[index] != null)
            return fields[index];

        return openField(index, false, null, null);
    }

    public final boolean isCached() {
        return (flags & FLAG_NONCACHED) == 0;
    }

    @Override
    public final boolean isModified() {
        return (flags & FLAG_MODIFIED) != 0;
    }

    @Override
    public final void setModified() {
        if (isModified())
            return;

        Assert.checkState(!isReadOnly());

        space.getNodeManager().onNodeModified(this);
        flags |= FLAG_MODIFIED;

        if (schema.getVersionField() != null) {
            VersionField field = getField(schema.getVersionField().getIndex());
            field.set(field.get() + 1);
        }
    }

    @Override
    public boolean isReadOnly() {
        return ((flags & FLAG_STALE) != 0) || ((flags & FLAG_NONCACHED) != 0) || space.getRawTransaction().isReadOnly() ||
                space.isClosed() || !object.allowModify();
    }

    public final boolean isStale() {
        boolean stale = (flags & FLAG_STALE) != 0;
        if (stale || refreshIndex == space.getNodeCache().getRefreshIndex())
            return stale;

        return refreshStale();
    }

    @Override
    public final long getId() {
        return nodeBlockIndex;
    }

    @Override
    public INodeSchema getSchema() {
        return schema;
    }

    @Override
    public final <T> T getObject() {
        return (T) object;
    }

    @Override
    public final int getFieldCount() {
        return schema.getFields().size();
    }

    @Override
    public final <T> T getField(int index) {
        return getFieldInstance(index).getObject();
    }

    @Override
    public final <T> T getField(IFieldSchema schema) {
        return getFieldInstance(schema.getIndex()).getObject();
    }

    public void refresh() {
        Assert.checkState(!isStale());
    }

    @Override
    public void updateCacheSize(int delta) {
        cacheSize += delta;
        Assert.checkState(cacheSize >= 0);

        if (element.isAttached() && !element.isRemoved())
            space.getNodeCache().updateCacheSize(this, delta);
    }

    public final void flush() {
        object.onBeforeFlush();

        if (schema.getConfiguration().isFullTextIndexed()) {
            if ((flags & FLAG_NEW) != 0)
                getSpace().getFullTextIndex().add(this);
            else {
                boolean updateIndex = false;
                if (fields != null) {
                    for (int i = 0; i < fields.length; i++) {
                        IField field = fields[i];
                        if (field != null && field.getSchema().getConfiguration().isFullTextIndexed() && ((IFullTextField) field.getObject()).isModified()) {
                            updateIndex = true;
                            break;
                        }
                    }
                }

                if (updateIndex)
                    getSpace().getFullTextIndex().update(this);
            }
        }

        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                IField field = fields[i];
                if (field instanceof SimpleField) {
                    SimpleField simpleField = (SimpleField) field;
                    simpleField.flush();
                } else if (field != null) {
                    ComplexField complexField = (ComplexField) field;
                    complexField.flush();
                }
            }
        }

        object.onAfterFlush();

        flags &= ~(FLAG_MODIFIED | FLAG_NEW);

        if (writeElement != null) {
            writeElement.remove();
            writeElement = null;
        }

        if (committedElement != null) {
            committedElement.remove();
            committedElement = null;
        }
    }

    public void clearModified() {
        flags &= ~FLAG_MODIFIED;
        writeElement = null;
    }

    public void setNonCached() {
        flags |= FLAG_NONCACHED;
    }

    public final void validate() {
        if (isModified())
            schema.validate(this);

        object.validate();
    }

    public final void unload() {
        object.onUnloaded();

        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i] != null)
                    ((IFieldObject) fields[i]).onUnloaded();
            }
        }

        if (writeElement != null) {
            writeElement.remove();
            writeElement = null;
        }

        if (committedElement != null) {
            committedElement.remove();
            committedElement = null;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Node))
            return false;

        Node node = (Node) o;
        return nodeBlockIndex == node.nodeBlockIndex && getFileIndex() == node.getFileIndex();
    }

    @Override
    public final int hashCode() {
        return 31 * (int) (nodeBlockIndex ^ (nodeBlockIndex >>> 32)) + getFileIndex();
    }

    public final IRawPage getPage(int fileIndex, long pageIndex) {
        if (headerPage.getIndex() == pageIndex && headerPage.getFile().getIndex() == fileIndex)
            return headerPage;

        return getRawTransaction().getPage(fileIndex, pageIndex);
    }

    @Override
    public IRawTransaction getRawTransaction() {
        return space.getRawTransaction();
    }

    @Override
    public ITransaction getTransaction() {
        return space.getTransaction();
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    public final String getSpaceFilesPath() {
        return space.getFilesPath();
    }

    public final Map<String, String> getProperties() {
        return space.getProperties(schema.getConfiguration());
    }

    public abstract int getFileIndex();

    public abstract int getDeletionCount();

    public abstract long allocateArea(IRawPage preferredPage);

    public abstract void freeArea(IRawPage page, int pageOffset);

    public abstract int allocateFile();

    public abstract long getRefId();

    public abstract Node open(long refId);

    public abstract boolean canReference(IReferenceFieldSchema fieldSchema, Node node);

    public abstract <T> T getRootNode();

    public abstract INodeLoader getNodeLoader();

    protected final IField openField(int index, boolean create, Object primaryKey, Object initializer) {
        IField field;
        if (schema.getFields().get(index).getConfiguration() instanceof SimpleFieldSchemaConfiguration)
            field = SimpleField.open(this, index, create, primaryKey, initializer);
        else if (schema.getFields().get(index).getConfiguration() instanceof ComplexFieldSchemaConfiguration)
            field = ComplexField.open(this, index, create, primaryKey, initializer);
        else
            return Assert.error();

        if (fields == null)
            fields = new IField[schema.getFields().size()];

        fields[index] = field;

        return field;
    }

    protected boolean refreshStale() {
        if ((flags & FLAG_STALE) != 0)
            return true;
        else {
            refreshIndex = space.getNodeCache().renewNode(this, true);
            return false;
        }
    }
}
