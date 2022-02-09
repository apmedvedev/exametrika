/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.RecurrentJobSchemaConfiguration;
import com.exametrika.common.tasks.IActivationCondition;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;

/**
 * The {@link JobActivationCondition} is a job activation condition.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobActivationCondition implements IActivationCondition<JobTask> {
    private final JobTask job;
    private final ISchedule schedule;
    private final ISchedulePeriod schedulePeriod;
    private final long maxRepeatCount;

    public JobActivationCondition(JobTask job) {
        Assert.notNull(job);

        this.job = job;
        schedule = job.getSchema().getSchedule().createSchedule();

        if (job.getSchema() instanceof RecurrentJobSchemaConfiguration) {
            RecurrentJobSchemaConfiguration recurrentConfiguration = (RecurrentJobSchemaConfiguration) job.getSchema();
            schedulePeriod = recurrentConfiguration.getPeriod().createPeriod();
            maxRepeatCount = recurrentConfiguration.getRepeatCount();
        } else {
            schedulePeriod = null;
            maxRepeatCount = Long.MAX_VALUE;
        }
    }

    @Override
    public boolean evaluate(Long value) {
        Assert.supports(false);
        return false;
    }

    @Override
    public boolean canActivate(long currentTime, ITaskContext context) {
        if (job.isActive())
            return false;

        if (job.getExecutionCount() >= maxRepeatCount)
            return false;

        if (job.getRestartCount() > 0) {
            if (job.getEndTime() + job.getSchema().getRestartPeriod() > currentTime)
                return false;
        } else {
            if (!schedule.evaluate(currentTime))
                return false;

            if (schedulePeriod != null && job.getEndTime() != 0 && !schedulePeriod.evaluate(job.getEndTime(), currentTime))
                return false;
        }

        if (job.getSchema().getGroup() != null) {
            if (context.getParameters().containsKey(job.getSchema().getGroup()))
                return false;

            context.getParameters().put(job.getSchema().getGroup(), this);
        }

        return true;
    }

    @Override
    public void tryInterrupt(long currentTime) {
        if (job.getSchema().getMaxExecutionPeriod() != 0 &&
                job.getStartTime() + job.getSchema().getMaxExecutionPeriod() < currentTime)
            job.cancel();
    }

    @Override
    public void onCompleted(ITaskContext context) {
        if (job.getSchema().getGroup() != null)
            Assert.checkState(context.getParameters().remove(job.getSchema().getGroup()) == this);
    }
}