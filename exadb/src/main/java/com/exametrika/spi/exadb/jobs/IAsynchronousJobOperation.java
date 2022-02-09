/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs;


/**
 * The {@link IAsynchronousJobOperation} represents an asynchronous job operation which must be completed using {@link IJobContext}
 * rather than completion of method {@link Runnable#run()}.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAsynchronousJobOperation extends Runnable {
}
