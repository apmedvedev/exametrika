/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IMeasurementStrategy} is a measurement strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementStrategy {
    /**
     * Allows or denies measurements.
     *
     * @return true if measurements are allowed
     */
    boolean allow();
}
