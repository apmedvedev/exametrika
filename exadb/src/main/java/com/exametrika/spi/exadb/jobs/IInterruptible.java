/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs;


/**
 * The {@link IInterruptible} represents an interruptible job operation.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInterruptible extends Runnable {
    /**
     * Marks job as interrupted.
     */
    void interrupt();
}
