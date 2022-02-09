/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link ILogProvider} represents a log provider.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ILogProvider {
    /**
     * Returns measurement value for specified log event.
     *
     * @param value log event
     * @return measurement value or null if provider does not have value to measure
     */
    Object getValue(ILogEvent value);
}
