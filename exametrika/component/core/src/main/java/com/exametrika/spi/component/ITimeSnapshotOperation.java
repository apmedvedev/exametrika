/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;


/**
 * The {@link ITimeSnapshotOperation} is a time snapshot transaction operation.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITimeSnapshotOperation {
    /**
     * Returns snapshot time.
     *
     * @return snapshot time
     */
    long getTime();

    /**
     * Sets snapshot time.
     *
     * @param time snapshot time
     */
    void setTime(long time);
}
