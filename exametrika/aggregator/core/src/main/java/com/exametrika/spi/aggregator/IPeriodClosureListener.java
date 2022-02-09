/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.IPeriod;


/**
 * The {@link IPeriodClosureListener} represents a listener of period closure event.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodClosureListener {
    /**
     * Called before specified period is closed.
     *
     * @param period closing period
     */
    void onBeforePeriodClosed(IPeriod period);
}
