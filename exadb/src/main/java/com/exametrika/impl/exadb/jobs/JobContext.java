/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;

/**
 * The {@link JobContext} is a job context.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobContext implements IJobContext {
    private final JobTask job;
    private boolean completed;
    private final JobManager jobManager;

    public JobContext(JobTask job, JobManager jobManager) {
        Assert.notNull(job);
        Assert.notNull(jobManager);

        this.job = job;
        this.jobManager = jobManager;
    }

    @Override
    public IDatabaseContext getDatabaseContext() {
        return jobManager.getContext();
    }

    @Override
    public JobSchemaConfiguration getSchema() {
        return job.getSchema();
    }

    @Override
    public boolean isPredefined() {
        return job.isPredefined();
    }

    @Override
    public void execute(final Runnable operation) {
        jobManager.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    operation.run();
                } catch (Exception e) {
                    onFailed(e);
                }
            }
        });
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void onSucceeded(Object value) {
        synchronized (this) {
            if (completed)
                return;

            completed = true;
        }

        job.onSucceeded(value);
    }

    @Override
    public void onFailed(Throwable error) {
        synchronized (this) {
            if (completed)
                return;

            completed = true;
        }

        job.onFailed(error);
    }

    @Override
    public boolean isAsync() {
        return job.getOperation() instanceof IAsynchronousJobOperation;
    }
}