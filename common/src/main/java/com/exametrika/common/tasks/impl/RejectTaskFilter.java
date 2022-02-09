/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.ITaskFilter;

/**
 * The {@link RejectTaskFilter} is a {@link ITaskFilter} implementation that rejects all incoming tasks.
 *
 * @param <T> task type
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 * @see ITaskFilter
 */
public final class RejectTaskFilter<T> implements ITaskFilter<T> {
    @Override
    public boolean accept(T task) {
        return false;
    }
}
