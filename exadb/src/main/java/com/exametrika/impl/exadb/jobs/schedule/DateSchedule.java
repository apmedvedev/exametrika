/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import com.exametrika.api.exadb.jobs.config.model.DateScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link DateSchedule} represents a date schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DateSchedule implements ISchedule {
    private final DateScheduleSchemaConfiguration configuration;

    public DateSchedule(DateScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean evaluate(long value) {
        boolean res = value >= configuration.getStartDate() && value <= configuration.getEndDate();
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}