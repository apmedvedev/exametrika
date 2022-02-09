/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.DayOfMonthScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link DayOfMonthSchedule} represents a day of month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfMonthSchedule implements ISchedule {
    private final DayOfMonthScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public DayOfMonthSchedule(DayOfMonthScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);

        current.setTimeInMillis(value);
        int dayOfMonth = current.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = current.getActualMaximum(Calendar.DAY_OF_MONTH);

        int startDay = configuration.getStartDay().getIndex(daysInMonth);
        int endDay = configuration.getEndDay().getIndex(daysInMonth);

        boolean res = dayOfMonth >= startDay && dayOfMonth <= endDay;
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}