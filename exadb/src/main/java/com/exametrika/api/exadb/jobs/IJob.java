/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;


/**
 * The {@link IJob} represents a job.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJob {
    /**
     * Returns job schema configuration.
     *
     * @return job configuration
     */
    JobSchemaConfiguration getJobSchema();

    /**
     * Is job predefined? Predefined jobs are set in configuration and can not be explicitly deleted.
     *
     * @return true if job is predefined
     */
    boolean isPredefined();

    /**
     * Is job active (executing)?
     *
     * @return true if job is active
     */
    boolean isActive();

    /**
     * Returns job last start time.
     *
     * @return job last start time
     */
    long getLastStartTime();

    /**
     * Returns job last end time.
     *
     * @return job last end time
     */
    long getLastEndTime();

    /**
     * Returns number of job executions (restarts are not included).
     *
     * @return number of job executions
     */
    long getExecutionCount();

    /**
     * Returns number of job restarts since last successful job execution.
     *
     * @return number of job restarts since last successful job execution
     */
    int getRestartCount();

    /**
     * Returns job executions.
     *
     * @return job executions. Latest executions comes first
     */
    Iterable<IJobExecution> getExecutions();

    /**
     * Clears all executions older than specified retention count.
     *
     * @param retentionCount number of executions retained in job execution history
     */
    void clearExecutions(int retentionCount);

    /**
     * Cancels current job execution (if any).
     */
    void cancel();

    /**
     * Deletes job.
     */
    void delete();
}
