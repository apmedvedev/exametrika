/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;


/**
 * The {@link IFilteredTaskQueue} is a task queue that can filter incoming tasks.
 *
 * @param <T> task type
 * @author Medvedev_A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFilteredTaskQueue<T> extends ITaskQueue<T> {
    /**
     * Adds task filter.
     *
     * @param filter task filter
     */
    void addFilter(ITaskFilter<T> filter);

    /**
     * Removes task filter.
     *
     * @param filter task filter
     */
    void removeFilter(ITaskFilter<T> filter);

    /**
     * Removes all task filters.
     */
    void removeAllFilters();
}
