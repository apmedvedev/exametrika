/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs;


/**
 * The {@link ISchedulePeriod} represents a schedule period.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchedulePeriod {
    /**
     * Returns true if this period is elapsed between end and start time.
     *
     * @param startTime start time
     * @param endTime   end time
     * @return true if this period is elapsed between end and start time
     */
    boolean evaluate(long startTime, long endTime);
}
