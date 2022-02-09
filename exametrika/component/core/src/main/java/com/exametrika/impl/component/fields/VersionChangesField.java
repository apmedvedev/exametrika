/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;

import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.objectdb.fields.BlobFieldInitializer;
import com.exametrika.impl.exadb.objectdb.fields.StructuredBlobField;
import com.exametrika.spi.component.IVersionChangeRecord;
import com.exametrika.spi.component.IVersionChangeRecord.Type;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link VersionChangesField} is a version changes field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VersionChangesField extends StructuredBlobField<IVersionChangeRecord> {
    public VersionChangesField(ISimpleField field) {
        super(field);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        BlobFieldInitializer blobFieldInitializer = new BlobFieldInitializer();
        blobFieldInitializer.setStore(getNode().getObject());
        super.onCreated(primaryKey, blobFieldInitializer);
    }

    @Override
    public void setStore(Object store) {
    }

    @Override
    protected void checkClass(Object record) {
        Assert.isTrue(record instanceof IVersionChangeRecord);
    }

    @Override
    protected Object doRead(IDataDeserialization fieldDeserialization) {
        int nodeSchemaIndex = fieldDeserialization.readInt();
        long time = fieldDeserialization.readLong();
        Type type = Serializers.readEnum(fieldDeserialization, Type.class);
        long scopeId = fieldDeserialization.readLong();
        long groupScopeId = fieldDeserialization.readLong();
        long nodeId = fieldDeserialization.readLong();
        long prevVersionNodeId = fieldDeserialization.readLong();

        return new VersionChangeRecord(nodeSchemaIndex, time, type, scopeId, groupScopeId, nodeId, prevVersionNodeId);
    }

    @Override
    protected void doWrite(IDataSerialization fieldSerialization, Object r) {
        IVersionChangeRecord record = (IVersionChangeRecord) r;

        fieldSerialization.writeInt(record.getNodeSchemaIndex());
        fieldSerialization.writeLong(record.getTime());
        Serializers.writeEnum(fieldSerialization, record.getType());
        fieldSerialization.writeLong(record.getScopeId());
        fieldSerialization.writeLong(record.getGroupScopeId());
        fieldSerialization.writeLong(record.getNodeId());
        fieldSerialization.writeLong(record.getPreviousVersionNodeId());
    }
}
