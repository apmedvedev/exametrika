/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ComponentSerializer} is an implementation of {@link IComponentValueSerializer} for component values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentSerializer implements IComponentValueSerializer {
    private final boolean builder;
    private final List<IMetricValueSerializer> metricSerializers;

    public ComponentSerializer(boolean builder, List<IMetricValueSerializer> metricSerializers) {
        Assert.notNull(metricSerializers);

        this.builder = builder;
        this.metricSerializers = metricSerializers;
    }

    @Override
    public void serialize(IDataSerialization serialization, IComponentValue value, boolean serializeMetadata) {
        Assert.isTrue(value.getMetrics().size() <= metricSerializers.size());

        serialization.writeInt(value.getMetrics().size());

        for (int i = 0; i < value.getMetrics().size(); i++) {
            IMetricValue metric = value.getMetrics().get(i);
            if (metric != null) {
                IMetricValueSerializer serializer = metricSerializers.get(i);

                serialization.writeBoolean(true);
                serializer.serialize(serialization, metric);
            } else {
                Assert.isTrue(!builder);
                serialization.writeBoolean(false);
            }
        }

        if (serializeMetadata)
            JsonSerializers.serialize(serialization, value.getMetadata());
    }

    @Override
    public IComponentValue deserialize(IDataDeserialization deserialization, boolean deserializeMetadata, JsonObject metadata) {
        int count = deserialization.readInt();
        Assert.isTrue(count <= metricSerializers.size());

        List<IMetricValue> metrics = new ArrayList<IMetricValue>(count);
        for (int i = 0; i < count; i++) {
            if (deserialization.readBoolean()) {
                IMetricValueSerializer serializer = metricSerializers.get(i);
                metrics.add(serializer.deserialize(deserialization));
            } else {
                Assert.isTrue(!builder);
                metrics.add(null);
            }
        }

        if (deserializeMetadata)
            metadata = JsonSerializers.deserialize(deserialization);

        if (builder)
            return new ComponentBuilder((List) metrics, metadata);
        else
            return new ComponentValue(metrics, metadata);
    }
}
