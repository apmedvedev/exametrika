/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link NameSerializer} is an implementation of {@link IMetricValueSerializer} for name values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameSerializer implements IMetricValueSerializer {
    private final boolean builder;
    private final List<IFieldValueSerializer> fieldSerializers;

    public NameSerializer(boolean builder, List<IFieldValueSerializer> fieldSerializers) {
        Assert.notNull(fieldSerializers);

        this.builder = builder;
        this.fieldSerializers = fieldSerializers;
    }

    @Override
    public void serialize(IDataSerialization serialization, IMetricValue value) {
        INameValue fieldValue = (INameValue) value;
        Assert.isTrue(fieldValue.getFields().size() <= fieldSerializers.size());

        serialization.writeInt(fieldValue.getFields().size());

        for (int i = 0; i < fieldValue.getFields().size(); i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            serializer.serialize(serialization, fieldValue.getFields().get(i));
        }
    }

    @Override
    public IMetricValue deserialize(IDataDeserialization deserialization) {
        int count = deserialization.readInt();
        Assert.isTrue(count <= fieldSerializers.size());

        List<IFieldValue> fields = new ArrayList<IFieldValue>(count);
        for (int i = 0; i < count; i++) {
            IFieldValueSerializer serializer = fieldSerializers.get(i);
            fields.add(serializer.deserialize(deserialization));
        }

        if (builder)
            return new NameBuilder((List) fields);
        else
            return new NameValue(fields);
    }
}
