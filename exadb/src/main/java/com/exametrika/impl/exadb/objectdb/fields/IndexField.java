/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.File;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IIndexField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link IndexField} is a index field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexField implements IIndexField, IFieldObject {
    private final ISimpleField field;
    private IIndex index;

    public IndexField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly() || (field.getNode().getTransaction().getOptions() & IOperation.DELAYED_FLUSH) != 0;
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public INode getNode() {
        return field.getNode();
    }

    @Override
    public <T> T get() {
        return getIndex();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        field.setModified();
    }

    @Override
    public <T extends IIndex> T getIndex() {
        field.refresh();

        if (index != null) {
            if (!index.isStale())
                return (T) index;
            else
                return (T) refreshIndex();
        } else
            return Assert.error();
    }

    @Override
    public void onCreated(Object primaryKey, Object initalizer) {
        Assert.isNull(primaryKey);

        IndexSchemaConfiguration indexConfiguration = ((IndexFieldSchemaConfiguration) getSchema().getConfiguration()).getIndex();
        String filePrefix = ((NodeSpace) getNode().getSpace()).getIndexesPath() + File.separator + indexConfiguration.getType();
        IIndexManager indexManager = getIndexManager();
        index = indexManager.createIndex(filePrefix, indexConfiguration);

        IRawWriteRegion region = field.getWriteRegion();
        region.writeInt(0, index.getId());
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        refreshIndex();
    }

    @Override
    public void onDeleted() {
        if (index != null) {
            if (index.isStale())
                refreshIndex();

            index.delete();
            index = null;
        }
    }

    @Override
    public void onUnloaded() {
        if (index != null) {
            index.unload();
            index = null;
        }
    }

    @Override
    public void flush() {
    }

    private IIndexManager getIndexManager() {
        NodeSpaceSchema spaceSchema = ((NodeSpaceSchema) field.getSchema().getParent().getParent());
        IDatabaseContext context = spaceSchema.getContext();
        return context.findTransactionExtension(IIndexManager.NAME);
    }

    private IIndex refreshIndex() {
        IRawReadRegion region = field.getReadRegion();
        int id = region.readInt(0);

        IIndexManager indexManager = getIndexManager();
        index = indexManager.getIndex(id);

        return index;
    }
}
