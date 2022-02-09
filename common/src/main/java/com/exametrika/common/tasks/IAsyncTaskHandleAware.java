/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link IAsyncTaskHandleAware} is used to set asynchronous task handle.
 *
 * @author Medvedev_A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAsyncTaskHandleAware {
    /**
     * Sets async task handle.
     *
     * @param taskHandle task handle
     */
    void setAsyncTaskHandle(Object taskHandle);
}
