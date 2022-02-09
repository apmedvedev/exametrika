/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;


/**
 * The {@link IComponentTypeAggregationSchema} represents an aggregation schema for component type.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentTypeAggregationSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    ComponentValueSchemaConfiguration getConfiguration();

    /**
     * Returns aggregator.
     *
     * @return aggregator
     */
    IComponentAggregator getAggregator();

    /**
     * Returns value serializer.
     *
     * @return value serializer
     */
    IComponentValueSerializer getValueSerializer();

    /**
     * Returns builder serializer.
     *
     * @return builder serializer
     */
    IComponentValueSerializer getBuilderSerializer();
}
