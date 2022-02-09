/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.IPeriodSpace;


/**
 * The {@link IArchivePolicy} represents an archive policy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IArchivePolicy {
    /**
     * Allows or denies archiving of specified current period space. Essentially this policy controls whether current period
     * space be closed for archiving or not. If current period space is closed new current period space is created.
     *
     * @param space current period space
     * @return true if current period space can be archived (i.e. closed for archiving)
     */
    boolean allow(IPeriodSpace space);
}
