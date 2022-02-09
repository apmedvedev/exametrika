/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.index.ops.RebuildStatisticsOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link RebuildStatisticsOperationSchemaConfiguration} is a rebuild statistics operation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RebuildStatisticsOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    private final double keyRatio;
    private final long rebuildThreshold;

    public RebuildStatisticsOperationSchemaConfiguration(double keyRatio, long rebuildThreshold) {
        this.keyRatio = keyRatio;
        this.rebuildThreshold = rebuildThreshold;
    }

    public double getKeyRatio() {
        return keyRatio;
    }

    public long getRebuildThreshold() {
        return rebuildThreshold;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Runnable createOperation(IJobContext context) {
        return new RebuildStatisticsOperation(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RebuildStatisticsOperationSchemaConfiguration))
            return false;

        RebuildStatisticsOperationSchemaConfiguration configuration = (RebuildStatisticsOperationSchemaConfiguration) o;
        return keyRatio == configuration.keyRatio && rebuildThreshold == configuration.rebuildThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyRatio, rebuildThreshold);
    }
}
