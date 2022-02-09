/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import com.exametrika.api.exadb.jobs.config.model.LowMemoryScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link LowMemorySchedule} represents a low memory schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LowMemorySchedule implements ISchedule {
    private final LowMemoryScheduleSchemaConfiguration configuration;

    public LowMemorySchedule(LowMemoryScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean evaluate(long value) {
        boolean res = false;
        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (memoryUsage.getMax() != -1) {
            if (memoryUsage.getMax() - memoryUsage.getUsed() < configuration.getMinFreeSpace())
                res = true;
        }

        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}