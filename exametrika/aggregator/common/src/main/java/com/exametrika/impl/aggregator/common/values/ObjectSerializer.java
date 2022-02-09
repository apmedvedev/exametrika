/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ObjectSerializer} is an implementation of {@link IMetricValueSerializer} for object fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSerializer implements IMetricValueSerializer {
    private final boolean builder;

    public ObjectSerializer(boolean builder) {
        this.builder = builder;
    }

    @Override
    public void serialize(IDataSerialization serialization, IMetricValue value) {
        IObjectValue objectValue = (IObjectValue) value;
        JsonSerializers.serialize(serialization, objectValue.getObject());
    }

    @Override
    public IMetricValue deserialize(IDataDeserialization deserialization) {
        Object value = JsonSerializers.deserialize(deserialization);

        if (builder)
            return new ObjectBuilder(value);
        else
            return new ObjectValue(value);
    }
}
