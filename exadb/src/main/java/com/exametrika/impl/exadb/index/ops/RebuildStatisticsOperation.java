/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.ops;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.index.IIndexOperationManager;
import com.exametrika.api.exadb.index.config.schema.RebuildStatisticsOperationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link RebuildStatisticsOperation} is a rebuild statistics operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RebuildStatisticsOperation implements Runnable {
    private final RebuildStatisticsOperationSchemaConfiguration configuration;
    private final IJobContext context;

    public RebuildStatisticsOperation(RebuildStatisticsOperationSchemaConfiguration configuration, IJobContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void run() {
        IDatabase database = context.getDatabaseContext().getDatabase();
        IIndexOperationManager operationManager = database.findExtension(IIndexOperationManager.NAME);
        Assert.checkState(operationManager != null);
        operationManager.rebuildStatistics(configuration.getKeyRatio(), configuration.getRebuildThreshold(), false);
    }
}
