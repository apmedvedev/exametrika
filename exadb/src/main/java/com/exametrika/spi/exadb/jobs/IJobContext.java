/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link IJobContext} represents a job context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJobContext extends ICompletionHandler {
    /**
     * Returns database context.
     *
     * @return database context
     */
    IDatabaseContext getDatabaseContext();

    /**
     * Returns job schema configuration.
     *
     * @return job schema configuration
     */
    JobSchemaConfiguration getSchema();

    /**
     * Is job predefined?
     *
     * @return true if job is predefined
     */
    boolean isPredefined();

    /**
     * Is currently executed operation asynchronous. Asynchronous operation must be completed using job context's completion handler rather than
     * as part of exiting operation's run method.
     *
     * @return if true currently executed operation is asynchronous
     */
    boolean isAsync();

    /**
     * Asynchronously executes next part of asynchronous job operation.
     *
     * @param operation operation to execute
     */
    void execute(Runnable operation);
}
