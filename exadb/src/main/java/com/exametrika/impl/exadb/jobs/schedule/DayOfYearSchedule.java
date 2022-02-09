/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.DayOfYearScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link DayOfYearSchedule} represents a day of year schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfYearSchedule implements ISchedule {
    private final DayOfYearScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public DayOfYearSchedule(DayOfYearScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);

        current.setTimeInMillis(value);
        int dayOfYear = current.get(Calendar.DAY_OF_YEAR);
        int daysInYear = current.getActualMaximum(Calendar.DAY_OF_YEAR);

        int startDay = configuration.getStartDay().getIndex(daysInYear);
        int endDay = configuration.getEndDay().getIndex(daysInYear);

        boolean res = dayOfYear >= startDay && dayOfYear <= endDay;
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}