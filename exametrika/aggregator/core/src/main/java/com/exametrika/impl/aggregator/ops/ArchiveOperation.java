/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.ops;

import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.config.schema.ArchiveOperationSchemaConfiguration;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link ArchiveOperation} is a archive operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ArchiveOperation implements IAsynchronousJobOperation {
    private final ArchiveOperationSchemaConfiguration configuration;
    private final IJobContext context;

    public ArchiveOperation(ArchiveOperationSchemaConfiguration configuration, IJobContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void run() {
        IDatabase database = context.getDatabaseContext().getDatabase();

        IPeriodOperationManager operationManager = database.findExtension(IPeriodOperationManager.NAME);
        Assert.checkState(operationManager != null);

        operationManager.archiveCycles(configuration.getSpaceFilter(), configuration.getPeriods(),
                configuration.getArchivePolicy(), configuration.getArchiveStore(), context);
    }
}
