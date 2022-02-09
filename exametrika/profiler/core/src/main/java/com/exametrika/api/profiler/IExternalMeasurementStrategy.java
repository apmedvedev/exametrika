/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler;

import com.exametrika.spi.profiler.IMeasurementStrategy;


/**
 * The {@link IExternalMeasurementStrategy} represents a measurement strategy that is controlled by external process.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IExternalMeasurementStrategy extends IMeasurementStrategy {
    /**
     * Allows or denies measurements of this measurement strategy
     *
     * @param value true if measurements are allowed
     */
    public void setAllowed(boolean value);
}
