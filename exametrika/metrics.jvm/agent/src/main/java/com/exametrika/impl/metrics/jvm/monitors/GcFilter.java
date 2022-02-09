/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.MemoryUsage;
import java.util.Map;

import com.exametrika.api.metrics.jvm.config.GcFilterConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.sun.management.GcInfo;


/**
 * The {@link GcFilter} is a garbage collection events filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GcFilter implements ICondition<GcInfo> {
    private final GcFilterConfiguration configuration;

    public GcFilter(GcFilterConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean evaluate(GcInfo value) {
        if (value.getDuration() < configuration.getMinDuration())
            return false;

        long freedMemory = 0;
        for (Map.Entry<String, MemoryUsage> entry : value.getMemoryUsageBeforeGc().entrySet()) {
            MemoryUsage beforeUsage = entry.getValue();
            MemoryUsage afterUsage = value.getMemoryUsageAfterGc().get(entry.getKey());

            long freed = beforeUsage.getUsed() - afterUsage.getUsed();

            if (freed > 0)
                freedMemory += freed;
        }

        if (freedMemory < configuration.getMinBytes())
            return false;

        return true;
    }
}
