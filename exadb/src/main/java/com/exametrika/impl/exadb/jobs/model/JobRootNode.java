/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link JobRootNode} is a job root.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JobRootNode extends ObjectNodeObject {
    private static final int JOBS_FIELD = 0;

    public JobRootNode(INode node) {
        super(node);
    }

    public Iterable<IJob> getJobs() {
        IReferenceField<IJob> jobsField = getNode().getField(JOBS_FIELD);
        return jobsField;
    }

    public void addJob(JobNode job) {
        Assert.notNull(job);

        IReferenceField<JobNode> jobsField = getNode().getField(JOBS_FIELD);
        jobsField.add(job);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonJobs = false;
        for (IJob job : getJobs()) {
            if (!jsonJobs) {
                json.key("jobs");
                json.startArray();
                jsonJobs = true;
            }

            json.startObject();
            ((INodeObject) job).dump(json, context);
            json.endObject();
        }

        if (jsonJobs)
            json.endArray();
    }
}