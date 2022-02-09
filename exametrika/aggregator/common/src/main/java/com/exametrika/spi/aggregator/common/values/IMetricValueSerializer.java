/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;

/**
 * The {@link IMetricValueSerializer} represents a metric value serializer.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMetricValueSerializer {
    /**
     * Serializes value.
     *
     * @param serialization serialization
     * @param value         value to serialize
     */
    void serialize(IDataSerialization serialization, IMetricValue value);

    /**
     * Deserializes value.
     *
     * @param deserialization deserialization
     * @return deserialized value
     */
    IMetricValue deserialize(IDataDeserialization deserialization);
}
