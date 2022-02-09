/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


/**
 * The {@link IProfilerMeasurementHandler} represents a measurement handler that is used by profiler.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProfilerMeasurementHandler extends IMeasurementHandler {
    /**
     * Sets current measurement schema used by profiler. Handler must ignore measurements from other schemas.
     *
     * @param schema current measurement schema used by profiler
     */
    void setSchema(IAggregationSchema schema);
}
