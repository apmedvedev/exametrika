/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.ICacheable;


/**
 * The {@link IComponentValueBuilder} represents a mutable component value builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface IComponentValueBuilder extends IComponentValue, ICacheable {
    /**
     * Assigns value to this builder.
     *
     * @param value value to set
     */
    void set(IComponentValue value);

    /**
     * Sets metadata.
     *
     * @param metadata metadata
     */
    void setMetadata(JsonObject metadata);

    /**
     * Converts value to immutable implementation of {@link IComponentValue}.
     *
     * @return value
     */
    IComponentValue toValue();

    /**
     * Converts value to immutable implementation of {@link IComponentValue}.
     *
     * @param includeMetadata if true metadata are included
     * @return value
     */
    IComponentValue toValue(boolean includeMetadata);

    /**
     * Clears measurement results.
     */
    void clear();
}
