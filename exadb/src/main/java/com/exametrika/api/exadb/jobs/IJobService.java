/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;


/**
 * The {@link IJobService} represents a job service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJobService {
    String NAME = "system.JobService";

    /**
     * Returns all available jobs.
     *
     * @return all available jobs
     */
    Iterable<IJob> getJobs();

    /**
     * Finds job by name.
     *
     * @param name job name
     * @return job or null if job is not found
     */
    IJob findJob(String name);

    /**
     * Adds/updates job.
     *
     * @param schema job schema configuration
     * @return job
     */
    IJob addJob(JobSchemaConfiguration schema);

    /**
     * Asynchronously executes specified job.
     *
     * @param job job to execute
     */
    void execute(Runnable job);
}
