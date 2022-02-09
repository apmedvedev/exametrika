/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.JobServiceConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.jobs.model.JobNode;
import com.exametrika.impl.exadb.jobs.model.JobRootNode;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link JobService} is a job service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobService extends DomainService implements IJobService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(JobService.class);
    private JobServiceConfiguration configuration = new JobServiceConfiguration();
    private JobManager jobManager;

    public JobManager getJobManager() {
        return jobManager;
    }

    @Override
    public void setSchema(IDomainServiceSchema schema) {
        IDomainServiceSchema oldSchema = this.schema;
        this.schema = schema;
        if (jobManager != null)
            synchronizeJobs(((JobServiceSchemaConfiguration) oldSchema.getConfiguration()).getPredefinedJobs(),
                    ((JobServiceSchemaConfiguration) schema.getConfiguration()).getPredefinedJobs(), false);
    }

    @Override
    public DomainServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new JobServiceConfiguration();

        this.configuration = (JobServiceConfiguration) configuration;
        if (jobManager != null)
            jobManager.setConfiguration(this.configuration.getThreadCount(), this.configuration.getSchedulePeriod());
    }

    @Override
    public void start(IDatabaseContext context) {
        this.context = context;
        jobManager = new JobManager(configuration.getThreadCount(), configuration.getSchedulePeriod(), context, this);
        jobManager.start();
        synchronizeJobs(null, ((JobServiceSchemaConfiguration) schema.getConfiguration()).getPredefinedJobs(), true);
    }

    @Override
    public void stop() {
        if (jobManager != null) {
            jobManager.stop();
            jobManager = null;
        }
    }

    @Override
    public Iterable<IJob> getJobs() {
        IObjectSpace space = getSpace();
        JobRootNode root = space.getRootNode();
        return root.getJobs();
    }

    @Override
    public IJob findJob(String name) {
        IObjectSpace space = getSpace();
        INodeIndex<String, IJob> index = space.getIndex(space.getSchema().findNode("Job").findField("name"));
        return index.find(name);
    }

    @Override
    public IJob addJob(JobSchemaConfiguration schema) {
        return addJob(schema, false);
    }

    @Override
    public void execute(Runnable job) {
        Assert.checkState(jobManager != null);
        jobManager.execute(job);
    }

    public void onJobExecutionStarted(final String name, final long startTime) {
        context.getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobNode job = (JobNode) findJob(name);
                if (job != null)
                    job.onStarted(startTime);
            }
        });
    }

    public void onJobExecutionSucceeded(final String name, final long endTime, final Object result) {
        context.getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobNode job = (JobNode) findJob(name);
                if (job != null)
                    job.onSucceeded(endTime, result);
            }
        });
    }

    public void onJobExecutionFailed(final String name, final long endTime, final String error) {
        context.getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobNode job = (JobNode) findJob(name);
                if (job != null)
                    job.onFailed(endTime, error);
            }
        });
    }

    private IJob addJob(JobSchemaConfiguration schema, boolean predefined) {
        IObjectSpace space = getSpace();
        INodeSchema nodeSchema = space.getSchema().findNode("Job");

        JobNode job = space.findNode(schema.getName(), nodeSchema);
        if (job != null)
            jobManager.setJobConfiguration(schema, predefined);
        else {
            job = space.createNode(schema.getName(), nodeSchema);
            jobManager.addJob(schema, predefined);

            JobRootNode root = space.getRootNode();
            root.addJob(job);
        }

        job.setPredefined(predefined);
        job.setJobSchema(schema);

        return job;
    }

    private IObjectSpace getSpace() {
        IObjectSpaceSchema spaceSchema = schema.getParent().findSpace("jobs");
        IObjectSpace space = spaceSchema.getSpace();
        return space;
    }

    private void synchronizeJobs(Map<String, JobSchemaConfiguration> oldPredefinedJobs,
                                 Map<String, JobSchemaConfiguration> newPredefinedJobs, boolean start) {
        Map<String, IJob> existingPredefinedJobs = new HashMap<String, IJob>();
        for (IJob job : getJobs()) {
            if (start && job.isActive()) {
                ILocalizedMessage message = messages.jobFailedByScheduler(job.getJobSchema().getName());
                onJobExecutionFailed(job.getJobSchema().getName(), Times.getCurrentTime(), message.toString());

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, message);
            }

            boolean load = start;
            if (job.isPredefined()) {
                JobSchemaConfiguration newSchema = newPredefinedJobs.get(job.getJobSchema().getName());
                if (newSchema == null) {
                    ((JobNode) job).deletePredefined();
                    load = false;
                } else {
                    existingPredefinedJobs.put(job.getJobSchema().getName(), job);
                    if (!start) {
                        JobSchemaConfiguration oldSchema = oldPredefinedJobs.get(job.getJobSchema().getName());
                        Assert.notNull(oldSchema);
                        if (!newSchema.equals(oldSchema)) {
                            ((JobNode) job).setJobSchema(newSchema);
                            jobManager.setJobConfiguration(newSchema, true);
                            load = false;
                        }
                    }
                }
            }

            if (load)
                jobManager.loadJob(job);
        }

        for (Map.Entry<String, JobSchemaConfiguration> entry : newPredefinedJobs.entrySet()) {
            if (existingPredefinedJobs.containsKey(entry.getKey()))
                continue;

            addJob(entry.getValue(), true);
        }
    }

    private interface IMessages {
        @DefaultMessage("Job ''{0}'' has failed due to job scheduler failure.")
        ILocalizedMessage jobFailedByScheduler(String name);
    }
}
