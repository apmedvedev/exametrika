/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.IPeriodCycle;


/**
 * The {@link ITruncationPolicy} represents a truncation policy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITruncationPolicy {
    /**
     * Allows or denies deletion of files of specified period cycle.
     *
     * @param cycle period cycle
     * @return true if files of period cycle can be deleted
     */
    boolean allow(IPeriodCycle cycle);
}
