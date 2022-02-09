/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.impl.exadb.core.tx.DbBatchOperation.CacheConstraint;


/**
 * The {@link DbBatchOperationSerializer} is serializer for {@link DbBatchOperation}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DbBatchOperationSerializer extends AbstractSerializer {
    private static final UUID ID = UUID.fromString("d8095c13-994e-406e-b335-26b1862a387b");

    public DbBatchOperationSerializer() {
        super(ID, DbBatchOperation.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        DbBatchOperation operation = (DbBatchOperation) object;
        serialization.writeBoolean(operation.isCachingEnabled());
        serialization.writeObject(operation.getOperation());

        if (operation.getConstraints() != null) {
            serialization.writeBoolean(true);
            serialization.writeInt(operation.getConstraints().size());
            for (CacheConstraint constraint : operation.getConstraints()) {
                serialization.writeString(constraint.category);
                serialization.writeLong(constraint.maxCacheSize);
            }
        } else
            serialization.writeBoolean(false);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        boolean cachingEnabled = deserialization.readBoolean();
        IBatchOperation operation = deserialization.readObject();

        Set<CacheConstraint> constraints = null;
        if (deserialization.readBoolean()) {
            int count = deserialization.readInt();
            constraints = new HashSet<CacheConstraint>(count);
            for (int i = 0; i < count; i++)
                constraints.add(new CacheConstraint(deserialization.readString(), deserialization.readLong()));
        }

        return new DbBatchOperation(operation, cachingEnabled, constraints);
    }
}
