/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.aggregator.common.model.IName;


/**
 * The {@link IPeriodNameManager} represents a period name.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodNameManager {
    String NAME = IPeriodNameManager.class.getName();

    /**
     * Adds new (if name does not exist) or returns existing period name by measurement name.
     *
     * @param name measurement name
     * @return period name or null if name is root
     */
    IPeriodName addName(IName name);

    /**
     * Returns period name by name identifier.
     *
     * @param id name identifier
     * @return period name or null if period name is not found or id is 0 (name is root)
     */
    IPeriodName findById(long id);

    /**
     * Returns period name by measurement name.
     *
     * @param name measurement name
     * @return period name or null if period name is not found or name is root
     */
    IPeriodName findByName(IName name);
}
