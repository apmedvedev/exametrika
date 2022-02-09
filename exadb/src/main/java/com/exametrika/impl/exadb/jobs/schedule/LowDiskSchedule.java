/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.io.File;

import com.exametrika.api.exadb.jobs.config.model.LowDiskScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link LowDiskSchedule} represents a low disk schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LowDiskSchedule implements ISchedule {
    private final LowDiskScheduleSchemaConfiguration configuration;
    private final File file;

    public LowDiskSchedule(LowDiskScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.file = new File(configuration.getPath());
    }

    @Override
    public boolean evaluate(long value) {
        boolean res = file.getFreeSpace() < configuration.getMinFreeSpace();

        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}