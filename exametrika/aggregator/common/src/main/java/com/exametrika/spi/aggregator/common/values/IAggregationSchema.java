/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import java.util.List;


/**
 * The {@link IAggregationSchema} represents an aggregation schema.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationSchema {
    /**
     * Returns schema version.
     *
     * @return schema version
     */
    int getVersion();

    /**
     * Returns list of component types schemas.
     *
     * @return list of component types schemas
     */
    List<IComponentTypeAggregationSchema> getComponentTypes();

    /**
     * Finds component type aggregation schema by component type name
     *
     * @param componentType component type name
     * @return component type aggregation schema or null if component type aggregation schema is not found
     */
    IComponentTypeAggregationSchema findComponentType(String componentType);
}
