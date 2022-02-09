/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.ops;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.objectdb.IObjectOperationManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link CompactionOperation} is a compaction operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompactionOperation implements IAsynchronousJobOperation {
    private final IJobContext context;

    public CompactionOperation(IJobContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public void run() {
        IDatabase database = context.getDatabaseContext().getDatabase();

        IObjectOperationManager operationManager = database.findExtension(IObjectOperationManager.NAME);
        Assert.checkState(operationManager != null);
        operationManager.compact(context);
    }
}
