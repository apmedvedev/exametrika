/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.jobs.IJobContext;

/**
 * The {@link JobOperationSchemaConfiguration} represents a configuration of job operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class JobOperationSchemaConfiguration extends Configuration {
    public abstract boolean isAsync();

    public abstract Runnable createOperation(IJobContext context);
}