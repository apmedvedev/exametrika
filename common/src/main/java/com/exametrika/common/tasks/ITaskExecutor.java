/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link ITaskExecutor} is used to asynchronously execute tasks.
 *
 * @param <T> task type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITaskExecutor<T> {
    /**
     * Adds task listener.
     *
     * @param listener task listener
     */
    void addTaskListener(ITaskListener<T> listener);

    /**
     * Removes task listener.
     *
     * @param listener task listener
     */
    void removeTaskListener(ITaskListener<T> listener);

    /**
     * Removes all task listeners.
     */
    void removeAllTaskListeners();
}
