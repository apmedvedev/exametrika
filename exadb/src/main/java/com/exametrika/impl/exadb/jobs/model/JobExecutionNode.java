/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.jobs.IJobExecution;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;


/**
 * The {@link JobExecutionNode} is a job execution.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JobExecutionNode extends ObjectNodeObject implements IJobExecution {
    private static final int STATUS_FIELD = 0;
    private static final int START_FIELD = 1;
    private static final int END_FIELD = 2;
    private static final int ERROR_FIELD = 3;
    private static final int RESULT_FIELD = 4;
    private static final int PREV_EXECUTION_FIELD = 5;

    public JobExecutionNode(INode node) {
        super(node);
    }

    public JobExecutionNode getPrevExecution() {
        ISingleReferenceField<JobExecutionNode> field = getNode().getField(PREV_EXECUTION_FIELD);
        return field.get();
    }

    public void set(Status status, long startTime, long endTime, String error, Object result, JobExecutionNode prevExecution) {
        IPrimitiveField statusField = getNode().getField(STATUS_FIELD);
        statusField.setByte((byte) status.ordinal());

        IPrimitiveField startField = getNode().getField(START_FIELD);
        startField.setLong(startTime);

        IPrimitiveField endField = getNode().getField(END_FIELD);
        endField.setLong(endTime);

        IStringField errorField = getNode().getField(ERROR_FIELD);
        errorField.set(error);

        ISerializableField resultField = getNode().getField(RESULT_FIELD);
        resultField.set(result);

        ISingleReferenceField<JobExecutionNode> prevExecutionField = getNode().getField(PREV_EXECUTION_FIELD);
        prevExecutionField.set(prevExecution);
    }

    @Override
    public Status getStatus() {
        IPrimitiveField field = getNode().getField(STATUS_FIELD);
        return Status.values()[field.getByte()];
    }

    @Override
    public long getStartTime() {
        IPrimitiveField field = getNode().getField(START_FIELD);
        return field.getLong();
    }

    @Override
    public long getEndTime() {
        IPrimitiveField field = getNode().getField(END_FIELD);
        return field.getLong();
    }

    @Override
    public String getError() {
        IStringField field = getNode().getField(ERROR_FIELD);
        return field.get();
    }

    @Override
    public Object getResult() {
        ISerializableField field = getNode().getField(RESULT_FIELD);
        return field.get();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("status");
        json.value(getStatus().toString().toLowerCase());
        json.key("startTime");
        json.value(getStartTime());
        json.key("endTime");
        json.value(getEndTime());
        if (getError() != null) {
            json.key("error");
            json.value(getError());
        }
        if (getResult() != null) {
            json.key("result");
            JsonSerializers.write(json, getResult());
        }
    }
}