/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

/**
 * The {@link ITimeSource} is a time source.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITimeSource {
    /**
     * Returns a current time in nanoseconds.
     *
     * @return current time
     */
    long getCurrentTime();

    /**
     * Returns a current time in nanoseconds for specified thread.
     *
     * @param threadId identifier of thread
     * @return current time
     */
    long getCurrentTime(long threadId);
}
