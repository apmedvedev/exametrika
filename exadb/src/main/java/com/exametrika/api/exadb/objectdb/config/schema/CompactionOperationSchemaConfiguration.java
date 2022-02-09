/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.impl.exadb.objectdb.ops.CompactionOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link CompactionOperationSchemaConfiguration} is a database compaction operation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompactionOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Runnable createOperation(IJobContext context) {
        return new CompactionOperation(context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompactionOperationSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
