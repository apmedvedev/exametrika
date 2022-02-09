/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link StackSerializer} is an implementation of {@link IMetricValueSerializer} for stack field values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackSerializer implements IMetricValueSerializer {
    private final boolean builder;
    private final List<IFieldValueSerializer> fieldSerializers;

    public StackSerializer(boolean builder, List<IFieldValueSerializer> fieldSerializers) {
        Assert.notNull(fieldSerializers);

        this.builder = builder;
        this.fieldSerializers = fieldSerializers;
    }

    @Override
    public void serialize(IDataSerialization serialization, IMetricValue value) {
        IStackValue stackValue = (IStackValue) value;
        Assert.isTrue(stackValue.getInherentFields().size() <= fieldSerializers.size());
        Assert.isTrue(stackValue.getTotalFields().size() <= fieldSerializers.size());
        Assert.isTrue(stackValue.getInherentFields().size() == stackValue.getTotalFields().size());

        serialization.writeInt(stackValue.getInherentFields().size());

        for (int i = 0; i < stackValue.getInherentFields().size(); i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            serializer.serialize(serialization, stackValue.getInherentFields().get(i));
        }

        for (int i = 0; i < stackValue.getTotalFields().size(); i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            serializer.serialize(serialization, stackValue.getTotalFields().get(i));
        }
    }

    @Override
    public IMetricValue deserialize(IDataDeserialization deserialization) {
        int count = deserialization.readInt();
        Assert.isTrue(count <= fieldSerializers.size());

        List<IFieldValue> inherentFields = new ArrayList<IFieldValue>(count);
        for (int i = 0; i < count; i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            inherentFields.add(serializer.deserialize(deserialization));
        }

        List<IFieldValue> totalFields = new ArrayList<IFieldValue>(count);
        for (int i = 0; i < count; i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            totalFields.add(serializer.deserialize(deserialization));
        }

        if (builder)
            return new StackBuilder((List) inherentFields, (List) totalFields);
        else
            return new StackValue(inherentFields, totalFields);
    }
}
