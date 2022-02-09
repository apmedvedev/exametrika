/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.RecurrentJobSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITaskScheduler;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskScheduler;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link JobManager} is a job manager.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(JobManager.class);
    private final IDatabaseContext context;
    private final JobService jobService;
    private final TaskScheduler<Runnable> scheduler;

    public JobManager(int threadCount, long schedulePeriod, IDatabaseContext context, JobService store) {
        Assert.notNull(context);
        Assert.notNull(store);

        this.context = context;
        this.jobService = store;
        scheduler = new TaskScheduler<Runnable>(threadCount, new RunnableTaskHandler(), context.getTimeService(),
                schedulePeriod, "Job scheduling thread", "Job handling thread", null);
    }

    public IDatabaseContext getContext() {
        return context;
    }

    public ITaskScheduler<Runnable> getScheduler() {
        return scheduler;
    }

    public void setConfiguration(int threadCount, long schedulePeriod) {
        scheduler.setThreadCount(threadCount);
        scheduler.setSchedulePeriod(schedulePeriod);
    }

    public void execute(Runnable job) {
        scheduler.put(job);
    }

    public void start() {
        scheduler.start();
    }

    public void stop() {
        scheduler.stop();
    }

    public void addJob(JobSchemaConfiguration schema, boolean predefined) {
        if (!schema.isEnabled())
            return;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.jobAdded(schema.getName()));

        JobTask jobTask = new JobTask(jobService, schema, predefined, 0, 0, 0, 0, this);
        scheduler.addTask(schema.getName(), jobTask, jobTask.getActivationCondition(), schema instanceof RecurrentJobSchemaConfiguration,
                schema.getOperation().isAsync(), jobTask);
    }

    public void loadJob(IJob job) {
        if (!job.getJobSchema().isEnabled())
            return;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.jobLoaded(job.getJobSchema().getName()));

        JobTask jobTask = new JobTask(jobService,
                job.getJobSchema(), job.isPredefined(), job.getLastStartTime(),
                job.getLastEndTime(), job.getExecutionCount(), job.getRestartCount(), this);
        scheduler.addTask(job.getJobSchema().getName(), jobTask, jobTask.getActivationCondition(),
                job.getJobSchema() instanceof RecurrentJobSchemaConfiguration, job.getJobSchema().getOperation().isAsync(), jobTask);
    }

    public void removeJob(String name) {
        scheduler.removeTask(name);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.jobRemoved(name));
    }

    public void setJobConfiguration(JobSchemaConfiguration schema, boolean predefined) {
        JobTask job = (JobTask) scheduler.findTask(schema.getName());
        if (job != null)
            scheduler.removeTask(schema.getName());

        addJob(schema, predefined);
    }

    public void cancelJob(String name) {
        JobTask job = (JobTask) scheduler.findTask(name);
        if (job == null)
            return;

        job.cancel();
    }

    private interface IMessages {
        @DefaultMessage("Job ''{0}'' has been loaded.")
        ILocalizedMessage jobLoaded(String name);

        @DefaultMessage("Job ''{0}'' has been removed.")
        ILocalizedMessage jobRemoved(String name);

        @DefaultMessage("Job ''{0}'' has been added.")
        ILocalizedMessage jobAdded(String name);
    }
}
