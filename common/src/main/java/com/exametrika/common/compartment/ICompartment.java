/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.ILifecycle;

import java.util.List;


/**
 * The {@link ICompartment} is a compartment which executes most of application code (short running tasks) in single thread -
 * main thread of compartment and delegates execution of all long running tasks to pool of threads.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompartment extends ICompartmentMXBean, ILifecycle, IFlowController, ITimeService {
    /**
     * Is compartment started?
     *
     * @return true if compartment is started
     */
    boolean isStarted();

    /**
     * Returns compartment group.
     *
     * @return compartment group
     */
    @Override
    ICompartmentGroup getGroup();

    /**
     * Adds compartment timer processor.
     *
     * @param processor compartment processor
     */
    public void addTimerProcessor(ICompartmentTimerProcessor processor);

    /**
     * Removes compartment timer processor.
     *
     * @param processor compartment processor
     */
    public void removeTimerProcessor(ICompartmentTimerProcessor processor);

    /**
     * Adds compartment processor.
     *
     * @param processor compartment processor
     */
    public void addProcessor(ICompartmentProcessor processor);

    /**
     * Removes compartment processor.
     *
     * @param processor compartment processor
     */
    public void removeProcessor(ICompartmentProcessor processor);

    /**
     * Offers new short running task, which must be executed asynchronously in main compartment thread. Must be called
     * from a main thread of some other compartment.
     *
     * @param task task to execute
     */
    void offer(ICompartmentTask task);

    /**
     * Offers new short running task, which must be executed asynchronously in main compartment thread.
     *
     * @param task task to execute
     */
    void offer(Runnable task);

    /**
     * Offers list of new short running tasks, which must be executed asynchronously in main compartment thread. Must be called
     * from a main thread of some other compartment. List elements can be of {@link Runnable} or {@link ICompartmentTask} type.
     *
     * @param tasks tasks to execute
     */
    void offer(List<?> tasks);

    /**
     * Executes specified task asynchronously in one of the threads of compartment thread pool. Results of execution are returned
     * back to the main compartment thread.
     *
     * @param task long running task to execute
     * @return true if task has been accepted, false has been rejected due to capacity restrictions
     */
    boolean execute(ICompartmentTask task);

    /**
     * Executes specified task asynchronously in one of the threads of compartment thread pool.
     *
     * @param task long running task to execute
     * @return true if task has been accepted, false has been rejected due to capacity restrictions
     */
    boolean execute(Runnable task);

    /**
     * Wakes up compartment dispatcher unblocking main compartment thread.
     */
    void wakeup();
}
