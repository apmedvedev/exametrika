/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;


import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link StandardSerializer} is an implementation of {@link IFieldValueSerializer} for {@link StandardValue}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardSerializer implements IFieldValueSerializer {
    private final boolean builder;

    public StandardSerializer(boolean builder) {
        this.builder = builder;
    }

    @Override
    public void serialize(IDataSerialization serialization, IFieldValue value) {
        IStandardValue standardValue = (IStandardValue) value;
        serialization.writeLong(standardValue.getCount());
        serialization.writeLong(standardValue.getSum());
        serialization.writeLong(standardValue.getMin());
        serialization.writeLong(standardValue.getMax());
    }

    @Override
    public IFieldValue deserialize(IDataDeserialization deserialization) {
        long count = deserialization.readLong();
        long sum = deserialization.readLong();
        long min = deserialization.readLong();
        long max = deserialization.readLong();

        if (builder)
            return new StandardBuilder(count, min, max, sum);
        else
            return new StandardValue(count, min, max, sum);
    }
}
