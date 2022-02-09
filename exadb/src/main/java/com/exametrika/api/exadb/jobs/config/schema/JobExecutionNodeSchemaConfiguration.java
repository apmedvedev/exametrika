/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.impl.exadb.jobs.model.JobExecutionNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link JobExecutionNodeSchemaConfiguration} is a job execution node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobExecutionNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public JobExecutionNodeSchemaConfiguration() {
        super("JobExecution", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new JobExecutionNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return JobExecutionNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(new PrimitiveFieldSchemaConfiguration("status", DataType.BYTE),
                new PrimitiveFieldSchemaConfiguration("startTime", DataType.LONG),
                new PrimitiveFieldSchemaConfiguration("endTime", DataType.LONG),
                new StringFieldSchemaConfiguration("error", 256),
                new SerializableFieldSchemaConfiguration("result"),
                new SingleReferenceFieldSchemaConfiguration("prevExecution", "JobExecution"));
    }
}