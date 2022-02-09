/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.aggregator.schema.ICycleSchema;


/**
 * The {@link IPeriodCycle} represents a period cycle.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodCycle {
    /**
     * Returns schema of space.
     *
     * @return space schema
     */
    ICycleSchema getSchema();

    /**
     * Returns unique identifier of cycle within database.
     *
     * @return cycle identifier
     */
    String getId();

    /**
     * Is space of this period cycle archived?
     *
     * @return true if space of this period cycle is archived
     */
    boolean isArchived();

    /**
     * Is space of this period cycle deleted from local database?
     *
     * @return true if space of this period cycle is deleted from local database
     */
    boolean isDeleted();

    /**
     * Is space of this period cycle at least once restored from archive?
     *
     * @return true if space of this period cycle at least once is restored from local database
     */
    boolean isRestored();

    /**
     * Returns period cycle start time.
     *
     * @return period cycle start time
     */
    long getStartTime();

    /**
     * Returns period cycle end time.
     *
     * @return period cycle end time
     */
    long getEndTime();

    /**
     * Returns previous cycle.
     *
     * @return previous cycle or null if previous cycle is not available
     */
    IPeriodCycle getPreviousCycle();

    /**
     * Returns period's space.
     *
     * @return space of period or null if space for this period cycle does not exist (deleted)
     */
    IPeriodSpace getSpace();
}
