/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;


/**
 * The {@link IMeasurementIdProvider} represents a measurement identifier provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IMeasurementIdProvider {
    /**
     * Returns measurement identifier.
     *
     * @return measurement identifier
     */
    IMeasurementId get();
}
