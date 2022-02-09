/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.ops;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.config.schema.BackupOperationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link BackupOperation} is a backup operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BackupOperation implements IAsynchronousJobOperation {
    private final BackupOperationSchemaConfiguration configuration;
    private final IJobContext context;

    public BackupOperation(BackupOperationSchemaConfiguration configuration, IJobContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void run() {
        IDatabase database = context.getDatabaseContext().getDatabase();
        database.getOperations().backup(configuration.getArchiveStore(), context);
    }
}
