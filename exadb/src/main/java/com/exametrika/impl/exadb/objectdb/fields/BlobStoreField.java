/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link BlobStoreField} is a blob store field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobStoreField implements IBlobStoreField, IFieldObject {
    private final ISimpleField field;
    private final FileField file;
    private BlobSpace space;

    public BlobStoreField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
        this.file = new FileField(field, "pages.blob");
    }

    public BlobSpace getSpace() {
        return space;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion() && ((BlobStoreFieldSchemaConfiguration) getSchema().getConfiguration()).isAllowDeletion();
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
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        field.setModified();
    }

    @Override
    public <T> T get() {
        return null;
    }

    @Override
    public long getFreeSpace() {
        return space.getFreeSpace();
    }

    @Override
    public IBlob createBlob() {
        return space.createBlob();
    }

    @Override
    public IBlob openBlob(long blobId) {
        return space.openBlob(blobId);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);

        file.onCreated(primaryKey, new FileFieldInitializer());

        space = BlobSpace.create(this, file.getFileIndex());
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        file.onOpened();

        space = BlobSpace.open(this, file.getFileIndex());
    }

    @Override
    public void onDeleted() {
        space = null;
        file.onDeleted();
    }

    @Override
    public void onUnloaded() {
        file.onUnloaded();
    }

    @Override
    public void flush() {
        file.flush();
    }
}
