/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.aggregator.common.model.IName;


/**
 * The {@link IPeriodName} represents a period name.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodName {
    /**
     * Returns name's identifier.
     *
     * @return name's identifier
     */
    long getId();

    /**
     * Returns actual measurement name.
     *
     * @return actual measurement name
     */
    <T extends IName> T getName();

    /**
     * Is name stale?
     *
     * @return true if name is stale
     */
    boolean isStale();

    /**
     * Refreshes internal name cache position in order to prevent accidental unloading of unmodified or flushing of modified name
     * in big transaction.
     */
    void refresh();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
