/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonObject;

/**
 * The {@link IComponentValueSerializer} represents a component value serializer.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentValueSerializer {
    /**
     * Serializes value.
     *
     * @param serialization     serialization
     * @param value             value to serialize
     * @param serializeMetadata if true metadata are serialized
     */
    void serialize(IDataSerialization serialization, IComponentValue value, boolean serializeMetadata);

    /**
     * Deserializes value.
     *
     * @param deserialization     deserialization
     * @param deserializeMetadata if true metadata are deserialized
     * @param metadata            metadata if metadata are not deserializer or null if metadata are deserialized
     * @return deserialized value
     */
    IComponentValue deserialize(IDataDeserialization deserialization, boolean deserializeMetadata, JsonObject metadata);
}
