/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;


/**
 * The {@link ITaskSource} is a source of tasks handled by {@link ITaskExecutor}.
 *
 * @param <T> task type
 * @author Medvedev_A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITaskSource<T> {
    /**
     * Takes task from the queue. Blocks calling thread (which is thread from the thread pool) until
     * first available task is offered to the queue.
     *
     * @return task taken from the queue
     * @throws ThreadInterruptedException if thread has been interrupted by another thread
     */
    T take();
}
