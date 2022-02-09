/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.MonthScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link MonthSchedule} represents a day of month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonthSchedule implements ISchedule {
    private final MonthScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public MonthSchedule(MonthScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);

        current.setTimeInMillis(value);
        int month = current.get(Calendar.MONTH);

        boolean res = configuration.getMonths().get(month);
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}