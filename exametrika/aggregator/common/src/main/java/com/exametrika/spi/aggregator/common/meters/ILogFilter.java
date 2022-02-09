/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link ILogFilter} represents a log filter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ILogFilter {
    /**
     * Allows or denies specified log event.
     *
     * @param value log event
     * @return true if log event is allowed
     */
    boolean allow(ILogEvent value);
}
