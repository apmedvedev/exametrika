/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.ops;

import java.io.File;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.config.schema.SimpleTruncationPolicySchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.spi.aggregator.ITruncationPolicy;


/**
 * The {@link SimpleTruncationPolicy} is a simple truncation policy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTruncationPolicy implements ITruncationPolicy {
    private final SimpleTruncationPolicySchemaConfiguration configuration;

    public SimpleTruncationPolicy(SimpleTruncationPolicySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean allow(IPeriodCycle cycle) {
        long currentTime = Times.getCurrentTime();
        if (cycle.getEndTime() + configuration.getMinRetentionPeriod() > currentTime)
            return false;
        if (cycle.getEndTime() + configuration.getMaxRetentionPeriod() < currentTime)
            return true;

        if (new File(((CycleSchema) cycle.getSchema()).getContext().getConfiguration().getPaths().get(
                configuration.getPathIndex())).getFreeSpace() < configuration.getMinFreeSpace())
            return true;

        return false;
    }
}
