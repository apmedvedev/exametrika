/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComponentComputer;


/**
 * The {@link IComponentRepresentationSchema} represents a schema for aggregation component representation.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentRepresentationSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    ComponentRepresentationSchemaConfiguration getConfiguration();

    /**
     * Returns index in array of component representation schemas.
     *
     * @return index in array of component representation schemas
     */
    int getIndex();

    /**
     * Returns accessor factory.
     *
     * @return accessor factory
     */
    IComponentAccessorFactory getAccessorFactory();

    /**
     * Returns computer.
     *
     * @return computer
     */
    IComponentComputer getComputer();
}
