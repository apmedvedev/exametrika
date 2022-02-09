/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs;


/**
 * The {@link ISchedule} represents a schedule.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchedule {
    /**
     * Returns true if schedule matched against specified current time.
     *
     * @param currentTime current time
     * @return true if schedule matched against specified current time
     */
    boolean evaluate(long currentTime);
}
