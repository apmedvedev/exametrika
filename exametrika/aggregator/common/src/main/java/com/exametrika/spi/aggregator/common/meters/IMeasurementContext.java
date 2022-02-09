/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link IMeasurementContext} represents a runtime measurement context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementContext {
    /**
     * Returns schema version.
     *
     * @return schema version
     */
    int getSchemaVersion();

    /**
     * Returns measurement handler.
     *
     * @return measurement handler
     */
    IMeasurementHandler getMeasurementHandler();
}
