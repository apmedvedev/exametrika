/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.time;

/**
 * The {@link ITimeService} is a time service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITimeService {
    /**
     * Returns a current time in milliseconds.
     *
     * @return current time
     */
    long getCurrentTime();
}
