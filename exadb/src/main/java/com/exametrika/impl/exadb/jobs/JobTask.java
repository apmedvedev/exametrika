/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.IAsyncTaskHandleAware;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.exadb.jobs.IInterruptible;

/**
 * The {@link JobTask} is a job task.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobTask implements Runnable, IAsyncTaskHandleAware {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(JobTask.class);
    private final JobService jobService;
    private final JobSchemaConfiguration schema;
    private final boolean predefined;
    private final JobActivationCondition activationCondition;
    private final JobManager jobManager;
    private volatile Runnable operation;
    private volatile long startTime;
    private volatile long endTime;
    private volatile long executionCount;
    private volatile int restartCount;
    private volatile boolean canceled;
    private volatile boolean active;
    private Object asyncTaskHandle;

    public JobTask(JobService jobService, JobSchemaConfiguration schema,
                   boolean predefined, long lastStartTime, long lastEndTime, long executionCount, int restartCount, JobManager jobManager) {
        Assert.notNull(jobService);
        Assert.notNull(schema);
        Assert.notNull(jobManager);

        this.jobService = jobService;
        this.schema = schema;
        this.predefined = predefined;

        this.startTime = lastStartTime;
        this.endTime = lastEndTime;
        this.executionCount = executionCount;
        this.restartCount = restartCount;

        this.activationCondition = new JobActivationCondition(this);
        this.jobManager = jobManager;
    }

    public JobActivationCondition getActivationCondition() {
        return activationCondition;
    }

    public Runnable getOperation() {
        return operation;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public long getRestartCount() {
        return restartCount;
    }

    public JobSchemaConfiguration getSchema() {
        return schema;
    }

    public boolean isPredefined() {
        return predefined;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void run() {
        startTime = Times.getCurrentTime();
        canceled = false;
        active = true;

        JobContext context = new JobContext(this, jobManager);
        operation = schema.getOperation().createOperation(context);

        jobService.onJobExecutionStarted(schema.getName(), startTime);

        if (logger.isLogEnabled(LogLevel.DEBUG)) {
            if (restartCount == 0)
                logger.log(LogLevel.DEBUG, messages.jobStarted(schema.getName(), executionCount));
            else
                logger.log(LogLevel.DEBUG, messages.jobRestarted(schema.getName(), executionCount, restartCount));
        }

        try {
            operation.run();

            if (!context.isAsync())
                context.onSucceeded(null);
        } catch (Exception e) {
            context.onFailed(e);
        }
    }

    public void onSucceeded(Object result) {
        synchronized (this) {
            if (!active)
                return;

            active = false;
            endTime = Times.getCurrentTime();

            executionCount++;
            restartCount = 0;
            operation = null;
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.jobSucceeded(schema.getName(), result));

        if (!canceled)
            jobService.onJobExecutionSucceeded(schema.getName(), endTime, result);

        if (asyncTaskHandle != null)
            jobManager.getScheduler().onAsyncTaskSucceeded(asyncTaskHandle);
    }

    public void onFailed(Throwable error) {
        synchronized (this) {
            if (!active)
                return;

            active = false;
            endTime = Times.getCurrentTime();
            operation = null;

            if (restartCount < schema.getRestartCount())
                restartCount++;
            else {
                restartCount = 0;
                executionCount++;
            }
        }

        if (logger.isLogEnabled(LogLevel.ERROR))
            logger.log(LogLevel.ERROR, messages.jobFailed(schema.getName()), error);

        jobService.onJobExecutionFailed(schema.getName(), endTime, Exceptions.getStackTrace(error));

        if (asyncTaskHandle != null)
            jobManager.getScheduler().onAsyncTaskFailed(asyncTaskHandle, error);
    }

    public void cancel() {
        Runnable operation = this.operation;
        if (active && operation instanceof IInterruptible) {
            canceled = true;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.jobCanceled(schema.getName()));

            ((IInterruptible) operation).interrupt();
        }
    }

    @Override
    public void setAsyncTaskHandle(Object taskHandle) {
        Assert.notNull(taskHandle);
        Assert.isNull(this.asyncTaskHandle);
        this.asyncTaskHandle = taskHandle;
    }

    private interface IMessages {
        @DefaultMessage("Job ''{0}'' has failed.")
        ILocalizedMessage jobFailed(String name);

        @DefaultMessage("Job ''{0}'' has succeeded with result ''{1}''.")
        ILocalizedMessage jobSucceeded(String name, Object result);

        @DefaultMessage("Job ''{0}'' has been restarted. Execution count - {1}, restart count: {2}.")
        ILocalizedMessage jobRestarted(String name, long executionCount, int restartCount);

        @DefaultMessage("Job ''{0}'' has been started. Execution count - {1}.")
        ILocalizedMessage jobStarted(String name, long executionCount);

        @DefaultMessage("Job ''{0}'' has been canceled by user.")
        ILocalizedMessage jobCanceled(String name);
    }
}