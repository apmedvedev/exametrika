/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeSpace;


/**
 * The {@link IPeriodSpace} represents a periodic space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodSpace extends INodeSpace {
    /**
     * Returns schema of space.
     *
     * @return space schema
     */
    ICycleSchema getSchema();

    /**
     * Returns space start time.
     *
     * @return space start time
     */
    long getStartTime();

    /**
     * Returns space end time.
     *
     * @return space end time or 0 if space is current (is not ended yet)
     */
    long getEndTime();

    /**
     * Returns cycle period.
     *
     * @return cycle period
     */
    IPeriod getCyclePeriod();

    /**
     * Returns current space period.
     *
     * @return current space period
     */
    IPeriod getCurrentPeriod();

    /**
     * Returns number of periods of space
     *
     * @return number of periods of space
     */
    int getPeriodsCount();

    /**
     * Returns period by index.
     *
     * @param index period index
     * @return period
     */
    IPeriod getPeriod(int index);

    /**
     * Finds nearest period whose start time is less or equal than specified time.
     *
     * @param time period time
     * @return period or null if period is not found in this space
     */
    IPeriod findPeriod(long time);

    /**
     * Returns previous cycle.
     *
     * @return previous cycle or null if previous cycle does not exist
     */
    IPeriodCycle getPreviousCycle();

    /**
     * Returns fulltext index.
     *
     * @return fulltext index or null if space does not have fulltext index
     */
    INodeFullTextIndex getFullTextIndex();
}
