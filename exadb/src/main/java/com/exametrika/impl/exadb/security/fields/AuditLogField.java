/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.fields;

import com.exametrika.api.exadb.security.IAuditLog;
import com.exametrika.api.exadb.security.IAuditRecord;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.fields.BlobFieldInitializer;
import com.exametrika.impl.exadb.objectdb.fields.StructuredBlobField;
import com.exametrika.impl.exadb.security.AuditRecord;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link AuditLogField} is a audit log field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AuditLogField extends StructuredBlobField<IAuditRecord> {
    public AuditLogField(ISimpleField field) {
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
        Assert.supports(false);
    }

    @Override
    protected IStructuredIterable createIterable(long startId, long endId, boolean includeEnd, boolean direct) {
        if (startId != 0)
            return new AuditLog(startId, endId, includeEnd, direct);
        else
            return new AuditLog();
    }

    @Override
    protected void checkClass(Object record) {
        Assert.isTrue(record instanceof IAuditRecord);
    }

    @Override
    protected Object doRead(IDataDeserialization fieldDeserialization) {
        String user = fieldDeserialization.readString();
        String permission = fieldDeserialization.readString();
        String object = fieldDeserialization.readString();
        long time = fieldDeserialization.readLong();
        boolean succeeded = fieldDeserialization.readBoolean();
        return new AuditRecord(user, permission, object, time, succeeded);
    }

    @Override
    protected void doWrite(IDataSerialization fieldSerialization, Object r) {
        IAuditRecord record = (IAuditRecord) r;
        fieldSerialization.writeString(record.getUser());
        fieldSerialization.writeString(record.getPermission());
        fieldSerialization.writeString(record.getObject());
        fieldSerialization.writeLong(record.getTime());
        fieldSerialization.writeBoolean(record.isSucceeded());
    }

    private class AuditLog extends StructuredIterable<IAuditRecord> implements IAuditLog {
        public AuditLog() {
        }

        public AuditLog(long startId, long endId, boolean includeEnd, boolean direct) {
            super(startId, endId, includeEnd, direct);
        }

        @Override
        public void clear() {
            AuditLogField.this.clear();
        }
    }
}
