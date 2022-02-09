/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.model;

import java.util.Iterator;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobExecution;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.jobs.JobManager;
import com.exametrika.impl.exadb.jobs.JobService;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link JobNode} is a job.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JobNode extends ObjectNodeObject implements IJob {
    protected static final int NAME_FIELD = 0;
    private static final int CONFIGURATION_FIELD = 1;
    private static final int PREDEFINED_FIELD = 2;
    private static final int ACTIVE_FIELD = 3;
    private static final int LAST_START_TIME_FIELD = 4;
    private static final int LAST_END_TIME_FIELD = 5;
    private static final int EXECUTION_COUNT_FIELD = 6;
    private static final int RESTART_COUNT_FIELD = 7;
    private static final int LAST_EXECUTION_FIELD = 8;
    private final JobManager jobManager;

    public JobNode(INode node) {
        super(node);

        jobManager = ((JobService) getNode().getTransaction().findDomainService(IJobService.NAME)).getJobManager();
    }

    public void setPredefined(boolean value) {
        IPrimitiveField field = getNode().getField(PREDEFINED_FIELD);
        field.setBoolean(value);
    }

    public void onStarted(long startTime) {
        IPrimitiveField lastStartTimeField = getNode().getField(LAST_START_TIME_FIELD);
        lastStartTimeField.setLong(startTime);

        IPrimitiveField lastEndTimeField = getNode().getField(LAST_END_TIME_FIELD);
        lastEndTimeField.setLong(0);

        IPrimitiveField activeField = getNode().getField(ACTIVE_FIELD);
        activeField.setBoolean(true);
    }

    public void onSucceeded(long endTime, Object result) {
        IPrimitiveField lastStartTimeField = getNode().getField(LAST_START_TIME_FIELD);

        IPrimitiveField lastEndTimeField = getNode().getField(LAST_END_TIME_FIELD);
        lastEndTimeField.setLong(endTime);

        IPrimitiveField activeField = getNode().getField(ACTIVE_FIELD);
        activeField.setBoolean(false);

        IPrimitiveField executionCountField = getNode().getField(EXECUTION_COUNT_FIELD);
        executionCountField.setLong(executionCountField.getLong() + 1);

        IPrimitiveField restartCountField = getNode().getField(RESTART_COUNT_FIELD);
        restartCountField.setInt(0);

        ISingleReferenceField<JobExecutionNode> lastExecutionField = getNode().getField(LAST_EXECUTION_FIELD);

        IObjectNode node = getNode();
        JobExecutionNode execution = node.getSpace().findOrCreateNode(null, getNode().getSchema().getParent().findNode("JobExecution"));
        execution.set(IJobExecution.Status.SUCCEEDED, lastStartTimeField.getLong(), endTime, null, result, lastExecutionField.get());
        lastExecutionField.set(execution);
    }

    public void onFailed(long endTime, String error) {
        IPrimitiveField lastStartTimeField = getNode().getField(LAST_START_TIME_FIELD);

        IPrimitiveField lastEndTimeField = getNode().getField(LAST_END_TIME_FIELD);
        lastEndTimeField.setLong(endTime);

        IPrimitiveField activeField = getNode().getField(ACTIVE_FIELD);
        activeField.setBoolean(false);

        JobSchemaConfiguration configuration = getJobSchema();
        IPrimitiveField restartCountField = getNode().getField(RESTART_COUNT_FIELD);
        if (restartCountField.getInt() < configuration.getRestartCount())
            restartCountField.setInt(restartCountField.getInt() + 1);
        else {
            restartCountField.setInt(0);
            IPrimitiveField executionCountField = getNode().getField(EXECUTION_COUNT_FIELD);
            executionCountField.setLong(executionCountField.getLong() + 1);
        }

        ISingleReferenceField<JobExecutionNode> lastExecutionField = getNode().getField(LAST_EXECUTION_FIELD);

        IObjectNode node = getNode();
        JobExecutionNode execution = node.getSpace().findOrCreateNode(null, getNode().getSchema().getParent().findNode("JobExecution"));
        execution.set(IJobExecution.Status.FAILED, lastStartTimeField.getLong(), endTime, error, null, lastExecutionField.get());
        lastExecutionField.set(execution);
    }

    public void setJobSchema(JobSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        ISerializableField<JobSchemaConfiguration> field = getNode().getField(CONFIGURATION_FIELD);
        JobSchemaConfiguration oldConfiguration = field.get();
        if (configuration.equals(oldConfiguration))
            return;

        field.set(configuration);
    }

    @Override
    public JobSchemaConfiguration getJobSchema() {
        ISerializableField<JobSchemaConfiguration> field = getNode().getField(CONFIGURATION_FIELD);
        return field.get();
    }

    @Override
    public boolean isPredefined() {
        IPrimitiveField field = getNode().getField(PREDEFINED_FIELD);
        return field.getBoolean();
    }

    @Override
    public boolean isActive() {
        IPrimitiveField field = getNode().getField(ACTIVE_FIELD);
        return field.getBoolean();
    }

    @Override
    public long getLastStartTime() {
        IPrimitiveField field = getNode().getField(LAST_START_TIME_FIELD);
        return field.getLong();
    }

    @Override
    public long getLastEndTime() {
        IPrimitiveField field = getNode().getField(LAST_END_TIME_FIELD);
        return field.getLong();
    }


    @Override
    public long getExecutionCount() {
        IPrimitiveField field = getNode().getField(EXECUTION_COUNT_FIELD);
        return field.getLong();
    }

    @Override
    public int getRestartCount() {
        IPrimitiveField field = getNode().getField(RESTART_COUNT_FIELD);
        return field.getInt();
    }

    @Override
    public Iterable<IJobExecution> getExecutions() {
        ISingleReferenceField<JobExecutionNode> field = getNode().getField(LAST_EXECUTION_FIELD);
        JobExecutionNode lastExecution = field.get();
        return new JobExecutionIterable(lastExecution);
    }

    @Override
    public void clearExecutions(int retentionCount) {
        int i = 0;
        for (IJobExecution execution : getExecutions()) {
            if (i >= retentionCount)
                ((JobExecutionNode) execution).delete();
            i++;
        }
    }

    @Override
    public void cancel() {
        if (isActive()) {
            long endTime = Times.getCurrentTime();
            IPrimitiveField lastStartTimeField = getNode().getField(LAST_START_TIME_FIELD);

            IPrimitiveField lastEndTimeField = getNode().getField(LAST_END_TIME_FIELD);
            lastEndTimeField.setLong(endTime);

            IPrimitiveField activeField = getNode().getField(ACTIVE_FIELD);
            activeField.setBoolean(false);

            IPrimitiveField restartCountField = getNode().getField(RESTART_COUNT_FIELD);
            restartCountField.setInt(0);

            IPrimitiveField executionCountField = getNode().getField(EXECUTION_COUNT_FIELD);
            executionCountField.setLong(executionCountField.getLong() + 1);

            ISingleReferenceField<JobExecutionNode> lastExecutionField = getNode().getField(LAST_EXECUTION_FIELD);

            IObjectNode node = getNode();
            JobExecutionNode execution = node.getSpace().findOrCreateNode(null, getNode().getSchema().getParent().findNode("JobExecution"));
            execution.set(IJobExecution.Status.CANCELED, lastStartTimeField.getLong(), endTime, null, null, lastExecutionField.get());
            lastExecutionField.set(execution);

            jobManager.cancelJob(getJobSchema().getName());
        }
    }

    @Override
    public void delete() {
        Assert.supports(!isPredefined());

        deleteJob();
    }

    public void deletePredefined() {
        deleteJob();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        JobSchemaConfiguration schema = getJobSchema();
        json.key("name");
        json.value(schema.getName());
        if (schema.getDescription() != null) {
            json.key("description");
            json.value(schema.getDescription());
        }
        if (schema.getGroup() != null) {
            json.key("group");
            json.value(schema.getGroup());
        }
        json.key("parameters");
        JsonSerializers.write(json, JsonUtils.toJson(schema.getParameters()));
        json.key("schedule");
        json.value(getJobSchema().getSchedule());
        json.key("enabled");
        json.value(getJobSchema().isEnabled());
        json.key("predefined");
        json.value(isPredefined());
        json.key("active");
        json.value(isActive());
        json.key("lastStartTime");
        json.value(getLastStartTime());
        json.key("lastEndTime");
        json.value(getLastEndTime());
        json.key("executionCount");
        json.value(getExecutionCount());
        json.key("restartCount");
        json.value(getRestartCount());

        boolean jsonExecutions = false;
        for (IJobExecution jobExecution : getExecutions()) {
            if (!jsonExecutions) {
                json.key("executions");
                json.startArray();
                jsonExecutions = true;
            }

            json.startObject();
            ((INodeObject) jobExecution).dump(json, context);
            json.endObject();
        }

        if (jsonExecutions)
            json.endArray();
    }

    private void deleteJob() {
        jobManager.removeJob(getJobSchema().getName());

        for (IJobExecution execution : getExecutions())
            ((JobExecutionNode) execution).delete();

        getNode().delete();
    }

    private class JobExecutionIterable implements Iterable<IJobExecution> {
        private final JobExecutionNode lastExecution;

        public JobExecutionIterable(JobExecutionNode lastExecution) {
            this.lastExecution = lastExecution;
        }

        @Override
        public Iterator<IJobExecution> iterator() {
            return new JobExecutionIterator(lastExecution);
        }
    }

    private class JobExecutionIterator implements Iterator<IJobExecution> {
        private JobExecutionNode execution;

        public JobExecutionIterator(JobExecutionNode lastExecution) {
            this.execution = lastExecution;
        }

        @Override
        public boolean hasNext() {
            return execution != null;
        }

        @Override
        public IJobExecution next() {
            refresh();
            Assert.checkState(execution != null);

            IJobExecution res = execution;
            execution = execution.getPrevExecution();
            return res;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }
}