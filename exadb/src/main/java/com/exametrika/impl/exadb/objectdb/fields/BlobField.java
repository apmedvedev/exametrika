/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;
import com.exametrika.spi.exadb.objectdb.schema.IBlobFieldSchema;


/**
 * The {@link BlobField} is a blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobField implements IBlobField, IFieldObject {
    public static final int HEADER_SIZE = SingleReferenceField.HEADER_SIZE + 8;// singleReferenceHeader + blobId(long) 
    private static final int BLOB_ID_OFFSET = SingleReferenceField.HEADER_SIZE;
    private final ISimpleField field;
    private final SingleReferenceField reference;
    private long blobId;
    private IBlob blob;
    private boolean modified;

    public BlobField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
        this.reference = new SingleReferenceField(field);
    }

    public ISimpleField getField() {
        return field;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
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
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        modified = true;
        field.setModified();
    }

    @Override
    public IBlob get() {
        if (blob != null && reference.get() != null)
            return blob;
        else
            return readBlob();
    }

    @Override
    public void set(IBlob value) {
        Assert.checkState(!field.isReadOnly());

        if (value != null) {
            Assert.isTrue(((IBlobFieldSchema) field.getSchema()).getStore() == value.getStore().getSchema());

            reference.set(value.getStore().getNode().getObject());
            blob = value;
            blobId = value.getId();
        } else {
            if (this.blob == null)
                return;

            this.reference.set(null);
            this.blob = null;
            this.blobId = 0;
        }

        setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);

        reference.onCreated(primaryKey, null);

        BlobFieldInitializer blobFieldInitializer = (BlobFieldInitializer) initializer;
        INodeObject blobStore = blobFieldInitializer.getStore();
        if (blobStore == null && ((BlobFieldSchemaConfiguration) field.getSchema().getConfiguration()).getBlobStoreNodeType() == null) {
            blobStore = ((Node) getNode()).getRootNode();
            if (blobStore == null)
                blobStore = getNode().getObject();
        }

        if (blobStore != null) {
            IBlobStoreField blobStoreField = blobStore.getNode().getField(
                    ((IBlobFieldSchema) field.getSchema()).getStore().getIndex());
            IBlob blob = blobStoreField.createBlob();
            set(blob);
        }
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        reference.onOpened();
    }

    @Override
    public void onDeleted() {
        get();
        if (blob != null)
            blob.delete();

        reference.onDeleted();
        blob = null;
        blobId = 0;
        modified = false;
    }

    @Override
    public void onUnloaded() {
        reference.onUnloaded();
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        reference.flush();

        IRawWriteRegion region = field.getWriteRegion();
        region.writeLong(BLOB_ID_OFFSET, blobId);
        modified = false;
    }

    private IBlob readBlob() {
        if (modified)
            return blob;

        IRawReadRegion region = field.getReadRegion();
        blobId = region.readLong(BLOB_ID_OFFSET);
        blob = null;

        INodeObject node = (INodeObject) reference.get();
        if (node == null || blobId == 0) {
            if (blobId != 0 && !field.isReadOnly()) {
                field.getWriteRegion().writeLong(BLOB_ID_OFFSET, 0);
                blobId = 0;
            }
            return null;
        }

        IBlobFieldSchema schema = (IBlobFieldSchema) field.getSchema();
        BlobStoreField store = (BlobStoreField) (node.getNode().getField(schema.getStore().getIndex()));
        blob = store.getSpace().openBlob(blobId);

        return blob;
    }
}
