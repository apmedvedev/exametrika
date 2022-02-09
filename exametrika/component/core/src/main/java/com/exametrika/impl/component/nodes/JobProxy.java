/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobExecution;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;

/**
 * The {@link JobProxy} is job proxy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JobProxy implements IJob {
    private IJob job;
    private ComponentNode component;

    public JobProxy(IJob job, ComponentNode component) {
        Assert.notNull(job);
        Assert.notNull(component);

        this.job = job;
        this.component = component;
    }

    public IJob getJob() {
        return job;
    }

    @Override
    public JobSchemaConfiguration getJobSchema() {
        return job.getJobSchema();
    }

    @Override
    public boolean isPredefined() {
        return job.isPredefined();
    }

    @Override
    public boolean isActive() {
        return job.isActive();
    }

    @Override
    public long getLastStartTime() {
        return job.getLastStartTime();
    }

    @Override
    public long getLastEndTime() {
        return job.getLastEndTime();
    }

    @Override
    public long getExecutionCount() {
        return job.getExecutionCount();
    }

    @Override
    public int getRestartCount() {
        return job.getRestartCount();
    }

    @Override
    public Iterable<IJobExecution> getExecutions() {
        return job.getExecutions();
    }

    @Override
    public void clearExecutions(int retentionCount) {
        IPermission permission = component.getSchema().getEditJobsPermission();
        permission.beginCheck(this);

        job.clearExecutions(retentionCount);

        permission.endCheck();
    }

    @Override
    public void cancel() {
        IPermission permission = component.getSchema().getCancelJobsPermission();
        permission.beginCheck(this);

        job.cancel();

        permission.endCheck();
    }

    @Override
    public void delete() {
        IPermission permission = component.getSchema().getEditJobsPermission();
        permission.beginCheck(this);

        job.delete();

        permission.endCheck();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobProxy))
            return false;

        JobProxy proxy = (JobProxy) o;
        return job.equals(proxy.job);
    }

    @Override
    public int hashCode() {
        return job.hashCode();
    }

    @Override
    public String toString() {
        return job.toString();
    }
}