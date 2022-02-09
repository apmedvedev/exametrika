/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.cache.NodeObjectCache;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCache;
import com.exametrika.impl.exadb.objectdb.fields.BodyField;
import com.exametrika.impl.exadb.objectdb.fields.ComplexField;
import com.exametrika.impl.exadb.objectdb.fields.SimpleField;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.NodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.INodeBody;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;


/**
 * The {@link ObjectNode} is a object node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectNode extends Node implements IObjectNode {
    private static final int NODE_CACHE_SIZE = getNodeCacheSize();
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int MAGIC = 0x19 << 24;
    private static final int MAGIC_MASK = 0xFF << 24;
    private static final int SCHEMA_INDEX_MASK = 0xFFFFFF;
    private static final int DELETED = 0x1 << 24;
    private static final int ROOT_FLAG = 0x2 << 24;
    private static final int DELETION_COUNT_MASK = 0xFFFFFF;
    private static final int PREV_NODE_BLOCK_INDEX_OFFSET = 4;
    private static final int FLAGS_OFFSET = 12;
    private static final int NEXT_FREE_NODE_BLOCK_INDEX_OFFSET = 16;
    private static final byte FLAG_DELETED = 0x10;
    private static final byte FLAG_ROOT = 0x20;
    private Element<ObjectNode> deletedElement;
    private int deletionCount;// magic(byte) + schemaIndex(3 byte) + prevNodeBlockIndex(long) + flags(byte)
    // + deletionCount(3 byte) + nextFreeNodeBlockIndex(long) + padding(int)

    public static ObjectNode create(ObjectSpace space, int schemaIndex, long prevNodeBlockIndex, Object primaryKey, boolean root,
                                    Object[] args) {
        ObjectNode node = space.getNodeManager().findFreeNode(space.getFileIndex(), schemaIndex);
        if (node != null) {
            node.create(-1, primaryKey, true, false, args);
            return node;
        }

        long nodeBlockIndex = space.getFreeNodeBlockIndex(schemaIndex);
        if (nodeBlockIndex != 0)
            return readInstance(space, nodeBlockIndex, true, primaryKey, args, null);

        INodeSchema nodeSchema = space.getSchema().getNodes().get(schemaIndex);
        nodeBlockIndex = space.allocateBlocks(Constants.blockCount(nodeSchema.getConfiguration().getSize()));
        IRawPage headerPage = space.getRawTransaction().getPage(space.getFileIndex(), Constants.pageIndexByBlockIndex(nodeBlockIndex));
        node = new ObjectNode(space, nodeBlockIndex, headerPage, nodeSchema, root);

        node.object = node.schema.getConfiguration().createNode(node);
        if (node.object instanceof NodeObject) {
            NodeObjectCache objectCache = space.getNodeObjectCache();
            objectCache.put(node.getId(), (NodeObject) node.object);
        }

        node.create(prevNodeBlockIndex, primaryKey, false, root, args);

        return node;
    }

    public static ObjectNode open(ObjectSpace space, long nodeBlockIndex, NodeObject object) {
        return readInstance(space, nodeBlockIndex, false, null, null, object);
    }

    @Override
    public IObjectNodeSchema getSchema() {
        return (IObjectNodeSchema) super.getSchema();
    }

    @Override
    public int getFileIndex() {
        return getSpace().getFileIndex();
    }

    @Override
    public String toString() {
        Object key = getKey();
        return (key != null ? key : "") + " (" + getId() + "@" + getSpace().toString() + ")";
    }

    public Element<ObjectNode> getDeletedElement() {
        if (deletedElement == null)
            deletedElement = new Element<ObjectNode>(this);

        return deletedElement;
    }

    public void removeDeletedElement() {
        deletedElement.remove();
        deletedElement = null;
    }

    public long getPrevNodeBlockIndex() {
        return headerPage.getReadRegion().readLong(getHeaderOffset() + PREV_NODE_BLOCK_INDEX_OFFSET);
    }

    @Override
    public void setStale() {
        if (isDeleted()) {
            if (object instanceof NodeObject) {
                NodeObjectCache objectCache = getSpace().getNodeObjectCache();
                objectCache.remove(getId());
            }
        }
        if (deletedElement != null) {
            deletedElement.remove();
            deletedElement = null;
        }

        super.setStale();
    }

    @Override
    public int getDeletionCount() {
        return deletionCount;
    }

    @Override
    public boolean isReadOnly() {
        return super.isReadOnly() || isDeleted();
    }

    @Override
    public boolean allowDeletion() {
        return (flags & FLAG_ROOT) == 0 && object.allowDeletion();
    }

    @Override
    public boolean allowFieldDeletion() {
        return true;
    }

    @Override
    public ObjectSpace getSpace() {
        return (ObjectSpace) space;
    }

    @Override
    public Object getKey() {
        if (schema.getPrimaryField() != null)
            return ((IPrimaryField) getField(schema.getPrimaryField().getIndex())).getKey();
        else
            return null;
    }

    @Override
    public boolean isDeleted() {
        return (flags & FLAG_DELETED) != 0;
    }

    @Override
    public void delete() {
        if (isDeleted())
            return;

        Assert.checkState(!isReadOnly());
        Assert.supports(allowDeletion());

        flags = FLAG_DELETED;

        object.onDeleted();

        if (schema.getConfiguration().isFullTextIndexed())
            getSpace().getFullTextIndex().remove(this);

        if (fields == null)
            this.fields = new IField[schema.getFields().size()];

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null)
                openField(i, false, null, null);

            ((IFieldObject) fields[i]).onDeleted();
        }

        this.fields = null;

        getSpace().getNodeManager().onNodeDeleted(this);

        IRawWriteRegion region = headerPage.getWriteRegion();
        deletionCount++;

        int headerOffset = getHeaderOffset();
        region.writeInt(headerOffset + FLAGS_OFFSET, deletionCount | DELETED);

        long nextFreeNodeBlockIndex = ((ObjectSpace) space).setFreeNodeBlockIndex(schema.getIndex(), nodeBlockIndex);
        region.writeLong(headerOffset + NEXT_FREE_NODE_BLOCK_INDEX_OFFSET, nextFreeNodeBlockIndex);
    }

    @Override
    public long allocateArea(IRawPage preferredPage) {
        return getSpace().allocateArea(preferredPage);
    }

    @Override
    public void freeArea(IRawPage page, int pageOffset) {
        getSpace().freeArea(page, pageOffset);
    }

    @Override
    public int allocateFile() {
        return getSpace().getSchema().getContext().getSchemaSpace().allocateFile(space.getRawTransaction());
    }

    @Override
    public long getRefId() {
        return nodeBlockIndex;
    }

    @Override
    public ObjectNode open(long nodeBlockIndex) {
        ObjectNodeCache nodeCache = getSpace().getNodeCache();
        ObjectNode node = nodeCache.findById(getFileIndex(), nodeBlockIndex);
        if (node == null) {
            node = ObjectNode.open(getSpace(), nodeBlockIndex, null);
            if (node.isDeleted())
                return null;

            nodeCache.addNode(node, false);
        }

        return node;
    }

    @Override
    public boolean canReference(IReferenceFieldSchema fieldSchema, Node node) {
        if (fieldSchema.getExternalSpaceSchema() == null)
            return getFileIndex() == node.getFileIndex();
        else
            return node.getSchema().getParent() == fieldSchema.getExternalSpaceSchema();
    }

    @Override
    public <T> T getRootNode() {
        return getSpace().getRootNode();
    }

    @Override
    public INodeLoader getNodeLoader() {
        return getSpace();
    }

    @Override
    protected boolean refreshStale() {
        if ((flags & FLAG_STALE) != 0)
            return true;

        if (!isDeleted())
            super.refreshStale();

        return false;
    }

    private ObjectNode(ObjectSpace space, long nodeBlockIndex, IRawPage headerPage, INodeSchema schema, boolean root) {
        super(space, schema, nodeBlockIndex, headerPage, NODE_CACHE_SIZE);

        if (root)
            flags |= FLAG_ROOT;
    }

    private static ObjectNode readInstance(ObjectSpace space, long nodeBlockIndex, boolean create, Object primaryKey, Object[] args,
                                           NodeObject object) {
        IRawPage headerPage = space.getRawTransaction().getPage(space.getFileIndex(), Constants.pageIndexByBlockIndex(nodeBlockIndex));
        IRawReadRegion region = headerPage.getReadRegion();
        int pageOffset = Constants.pageOffsetByBlockIndex(nodeBlockIndex);

        int value = region.readInt(pageOffset);
        if ((value & MAGIC_MASK) != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(space.getFileIndex()));

        int schemaIndex = value & SCHEMA_INDEX_MASK;
        // skip prevNodeBlockIndex, nextFreeBlockIndex
        INodeSchema schema = space.getSchema().getNodes().get(schemaIndex);

        int flagsValue = region.readInt(pageOffset + FLAGS_OFFSET);
        boolean deleted = (flagsValue & DELETED) != 0;
        boolean root = (flagsValue & ROOT_FLAG) != 0;
        if (create && !deleted)
            throw new RawDatabaseException(messages.invalidFormat(space.getFileIndex()));

        ObjectNode node = new ObjectNode(space, nodeBlockIndex, headerPage, schema, root);

        if (deleted)
            node.flags |= FLAG_DELETED;
        node.deletionCount = (flagsValue & DELETION_COUNT_MASK);

        if (create) {
            node.object = schema.getConfiguration().createNode(node);
            if (node.object instanceof NodeObject) {
                NodeObjectCache objectCache = space.getNodeObjectCache();
                objectCache.put(node.getId(), (NodeObject) node.object);
            }
            node.create(-1, primaryKey, true, root, args);
        } else if (!deleted) {
            if (schema.getBodyField() != null) {
                BodyField field = node.getField(schema.getBodyField().getIndex());
                INodeBody body = field.readValue();
                body.setField(field);
                node.object = body;
            } else if (object != null)
                node.object = object;
            else
                node.object = node.createNodeObject(schema);

            node.object.onOpened();
        }

        return node;
    }

    private INodeObject createNodeObject(INodeSchema schema) {
        ObjectSpace space = getSpace();
        INodeObject object;
        if (space.getNodeManager().isCachingEnabled()) {
            NodeObjectCache objectCache = space.getNodeObjectCache();

            object = objectCache.get(getId());
            if (object != null) {
                ((NodeObject) object).init(this);
                return object;
            }

            object = schema.getConfiguration().createNode(this);
            if (object instanceof NodeObject)
                objectCache.put(getId(), (NodeObject) object);
        } else {
            setNonCached();
            object = schema.getConfiguration().createNode(this);
        }

        return object;
    }

    private void create(long prevNodeBlockIndex, Object primaryKey, boolean recreate, boolean root, Object[] args) {
        flags = FLAG_NEW | FLAG_MODIFIED;
        if (root)
            flags |= FLAG_ROOT;

        IRawWriteRegion region = headerPage.getWriteRegion();

        int headerOffset = getHeaderOffset();
        region.writeInt(headerOffset, MAGIC | schema.getIndex());
        if (prevNodeBlockIndex != -1)
            region.writeLong(headerOffset + PREV_NODE_BLOCK_INDEX_OFFSET, prevNodeBlockIndex);
        int value = region.readInt(headerOffset + FLAGS_OFFSET);
        deletionCount = (value & DELETION_COUNT_MASK);
        region.writeInt(headerOffset + FLAGS_OFFSET, deletionCount | (root ? ROOT_FLAG : 0));

        if (recreate) {
            long nextFreeNodeBlockIndex = region.readInt(headerOffset + NEXT_FREE_NODE_BLOCK_INDEX_OFFSET);
            Assert.checkState(((ObjectSpace) space).setFreeNodeBlockIndex(schema.getIndex(), nextFreeNodeBlockIndex) == nodeBlockIndex);

            cacheSize = schema.getConfiguration().getCacheSize() + NODE_CACHE_SIZE;
            refreshIndex = space.getNodeCache().getRefreshIndex();
        }

        region.writeLong(headerOffset + NEXT_FREE_NODE_BLOCK_INDEX_OFFSET, 0);

        if (fields == null)
            fields = new IField[schema.getFields().size()];

        Object[] fieldInitializers = new Object[fields.length];
        for (int i = 0; i < fields.length; i++)
            fieldInitializers[i] = schema.getFields().get(i).getConfiguration().createInitializer();

        object.onBeforeCreated(primaryKey, args, fieldInitializers);

        for (int i = 0; i < fields.length; i++) {
            FieldSchemaConfiguration fieldConfiguration = schema.getFields().get(i).getConfiguration();
            Object fieldPrimaryKey = fieldConfiguration.isPrimary() ? primaryKey : null;

            if (deletionCount == 0) {
                Assert.checkState(fields[i] == null);
                if (fieldConfiguration instanceof SimpleFieldSchemaConfiguration)
                    fields[i] = SimpleField.create(this, i, fieldPrimaryKey, fieldInitializers[i]);
                else if (fieldConfiguration instanceof ComplexFieldSchemaConfiguration)
                    fields[i] = ComplexField.create(this, i, fieldPrimaryKey, fieldInitializers[i]);
                else
                    Assert.error();
            } else {
                if (fields[i] == null)
                    openField(i, true, fieldPrimaryKey, fieldInitializers[i]);
                else
                    ((IFieldObject) fields[i]).onCreated(fieldPrimaryKey, fieldInitializers[i]);
            }
        }

        for (int i = 0; i < fields.length; i++) {
            FieldSchemaConfiguration fieldConfiguration = schema.getFields().get(i).getConfiguration();
            Object fieldPrimaryKey = fieldConfiguration.isPrimary() ? primaryKey : null;

            ((IFieldObject) fields[i]).onAfterCreated(fieldPrimaryKey, fieldInitializers[i]);
        }

        if (schema.getBodyField() != null) {
            BodyField field = getField(schema.getBodyField().getIndex());
            ((INodeBody) object).setField(field);
        }
    }

    private static int getNodeCacheSize() {
        return Memory.getShallowSize(ObjectNode.class) + 4 * Memory.getShallowSize(SimpleList.Element.class);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
