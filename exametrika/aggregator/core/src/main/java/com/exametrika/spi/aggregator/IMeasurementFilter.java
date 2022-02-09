/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.model.Measurement;


/**
 * The {@link IMeasurementFilter} represents a filter of measurements.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IMeasurementFilter {
    /**
     * Allows or denies specified measurement.
     *
     * @param measurement measurement
     * @return true if measurement is allowed
     */
    boolean allow(Measurement measurement);
}
