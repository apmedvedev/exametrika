/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.model.MeasurementSet;


/**
 * The {@link IMeasurementHandler} handles measurements.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementHandler {
    /**
     * Can measurement handler handle incoming measurements?
     *
     * @return true if measurement handler can handle incoming measurements
     */
    boolean canHandle();

    /**
     * Handles specified measurements.
     *
     * @param measurements measurements
     */
    void handle(MeasurementSet measurements);
}
