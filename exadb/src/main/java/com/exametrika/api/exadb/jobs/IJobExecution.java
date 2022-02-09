/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs;


/**
 * The {@link IJobExecution} represents a single job execution.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJobExecution {
    enum Status {
        /**
         * Job execution succeeded.
         */
        SUCCEEDED,

        /**
         * Job execution failed.
         */
        FAILED,

        /**
         * Job execution has been canceled by user.
         */
        CANCELED
    }

    /**
     * Returns execution status.
     *
     * @return execution status
     */
    Status getStatus();

    /**
     * Returns start time.
     *
     * @return start time
     */
    long getStartTime();

    /**
     * Returns end time.
     *
     * @return end time
     */
    long getEndTime();

    /**
     * Returns error description.
     *
     * @return error description or null if execution does not failed
     */
    String getError();

    /**
     * Returns result.
     *
     * @return result or null if result is not set
     */
    Object getResult();
}
