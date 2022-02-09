/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.impl.exadb.jobs.model.JobNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link JobNodeSchemaConfiguration} is a job node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public JobNodeSchemaConfiguration() {
        super("Job", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new JobNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return JobNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(new IndexedStringFieldSchemaConfiguration("name", true, 256),
                new SerializableFieldSchemaConfiguration("configuration"),
                new PrimitiveFieldSchemaConfiguration("predefined", DataType.BOOLEAN),
                new PrimitiveFieldSchemaConfiguration("active", DataType.BOOLEAN),
                new PrimitiveFieldSchemaConfiguration("lastStartTime", DataType.LONG),
                new PrimitiveFieldSchemaConfiguration("lastEndTime", DataType.LONG),
                new PrimitiveFieldSchemaConfiguration("executionCount", DataType.LONG),
                new PrimitiveFieldSchemaConfiguration("restartCount", DataType.INT),
                new SingleReferenceFieldSchemaConfiguration("lastExecution", "JobExecution"));
    }
}