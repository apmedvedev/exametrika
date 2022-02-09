/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Serializers;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link StackIdsSerializer} is an implementation of {@link IMetricValueSerializer} for stackIds values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackIdsSerializer implements IMetricValueSerializer {
    private final boolean builder;

    public StackIdsSerializer(boolean builder) {
        this.builder = builder;
    }

    @Override
    public void serialize(IDataSerialization serialization, IMetricValue value) {
        IStackIdsValue stackIdsValue = (IStackIdsValue) value;

        if (stackIdsValue.getIds() != null) {
            serialization.writeBoolean(true);
            serialization.writeInt(stackIdsValue.getIds().size());

            for (UUID id : stackIdsValue.getIds())
                Serializers.writeUUID(serialization, id);
        } else
            serialization.writeBoolean(false);
    }

    @Override
    public IMetricValue deserialize(IDataDeserialization deserialization) {
        Set<UUID> ids = null;
        if (deserialization.readBoolean()) {
            int count = deserialization.readInt();

            ids = new LinkedHashSet<UUID>(count);
            for (int i = 0; i < count; i++)
                ids.add(Serializers.readUUID(deserialization));
        }

        if (builder)
            return new StackIdsBuilder(ids);
        else
            return new StackIdsValue(ids);
    }
}
