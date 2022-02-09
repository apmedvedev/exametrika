/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link IMeasurementProvider} represents a measurement provider.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementProvider {
    /**
     * Returns measurement value.
     *
     * @return measurement value or null if provider does not have value to measure
     * @throws Exception if some exception occured
     */
    Object getValue() throws Exception;
}
