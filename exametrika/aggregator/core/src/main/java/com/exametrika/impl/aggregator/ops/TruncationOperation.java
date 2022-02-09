/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.ops;

import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.config.schema.TruncationOperationSchemaConfiguration;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link TruncationOperation} is a truncation operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TruncationOperation implements IAsynchronousJobOperation {
    private final TruncationOperationSchemaConfiguration configuration;
    private final IJobContext context;

    public TruncationOperation(TruncationOperationSchemaConfiguration configuration, IJobContext context) {
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

        operationManager.truncateCycles(configuration.getSpaceFilter(), configuration.getPeriods(),
                configuration.getTruncationPolicy(), true, context);
    }
}
