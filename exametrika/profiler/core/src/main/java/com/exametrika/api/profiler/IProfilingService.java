/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler;

import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProfilerMeasurementHandler;


/**
 * The {@link IProfilingService} represents a profiling service.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProfilingService {
    final String NAME = "profiler";

    /**
     * Returns aggregation schema of profiler.
     *
     * @return aggregation schema of profiler or null if aggregation schema is not set
     */
    IAggregationSchema getAggregationSchema();

    /**
     * Sets measurement handler.
     *
     * @param measurementHandler measurement handler
     */
    void setMeasurementHandler(IProfilerMeasurementHandler measurementHandler);

    /**
     * Finds measurement strategy by name.
     *
     * @param <T>  strategy type
     * @param name strategy name
     * @return measurement strategy or null if measurement strategy is not found
     */
    <T extends IMeasurementStrategy> T findMeasurementStrategy(String name);

    /**
     * Requests measurements extraction and send measurements to server.
     */
    void requestMeasurements();
}
