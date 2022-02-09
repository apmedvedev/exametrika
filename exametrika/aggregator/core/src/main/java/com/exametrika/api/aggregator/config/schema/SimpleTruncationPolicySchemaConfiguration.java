/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.ops.SimpleTruncationPolicy;
import com.exametrika.spi.aggregator.ITruncationPolicy;
import com.exametrika.spi.aggregator.config.schema.TruncationPolicySchemaConfiguration;


/**
 * The {@link SimpleTruncationPolicySchemaConfiguration} is a simple truncation policy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTruncationPolicySchemaConfiguration extends TruncationPolicySchemaConfiguration {
    private final long minRetentionPeriod;
    private final long maxRetentionPeriod;
    private final long minFreeSpace;
    private final int pathIndex;

    public SimpleTruncationPolicySchemaConfiguration(long minRetentionPeriod, long maxRetentionPeriod, long minFreeSpace, int pathIndex) {
        this.minRetentionPeriod = minRetentionPeriod;
        this.maxRetentionPeriod = maxRetentionPeriod;
        this.minFreeSpace = minFreeSpace;
        this.pathIndex = pathIndex;
    }

    public long getMinRetentionPeriod() {
        return minRetentionPeriod;
    }

    public long getMaxRetentionPeriod() {
        return maxRetentionPeriod;
    }

    public long getMinFreeSpace() {
        return minFreeSpace;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    @Override
    public ITruncationPolicy createPolicy() {
        return new SimpleTruncationPolicy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleTruncationPolicySchemaConfiguration))
            return false;

        SimpleTruncationPolicySchemaConfiguration configuration = (SimpleTruncationPolicySchemaConfiguration) o;
        return minRetentionPeriod == configuration.minRetentionPeriod &&
                maxRetentionPeriod == configuration.maxRetentionPeriod &&
                minFreeSpace == configuration.minFreeSpace && pathIndex == configuration.pathIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(minRetentionPeriod, maxRetentionPeriod, minFreeSpace, pathIndex);
    }
}
