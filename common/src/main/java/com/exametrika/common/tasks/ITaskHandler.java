/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link ITaskHandler} is used to handle incoming tasks.
 *
 * @param <T> task type
 * @author Medvedev_A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITaskHandler<T> {
    /**
     * Handles incoming task.
     *
     * @param task incoming task
     */
    void handle(T task);
}
