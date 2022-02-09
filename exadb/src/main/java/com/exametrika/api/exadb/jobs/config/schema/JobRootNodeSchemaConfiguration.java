/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.impl.exadb.jobs.model.JobRootNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link JobRootNodeSchemaConfiguration} is a job root node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobRootNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public JobRootNodeSchemaConfiguration() {
        super("Root", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new JobRootNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return JobRootNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(new ReferenceFieldSchemaConfiguration("jobs", "Job"));
    }
}