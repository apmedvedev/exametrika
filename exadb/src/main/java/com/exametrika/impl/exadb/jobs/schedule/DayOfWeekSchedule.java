/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.DayOfWeekScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link DayOfWeekSchedule} represents a day of week schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfWeekSchedule implements ISchedule {
    private final DayOfWeekScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public DayOfWeekSchedule(DayOfWeekScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);

        current.setTimeInMillis(value);

        int index = getDayOfWeekIndex(current);
        boolean res = configuration.getWeekDays().get(index);
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }

    private int getDayOfWeekIndex(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int index;
        if (calendar.getFirstDayOfWeek() == Calendar.MONDAY) {
            if (dayOfWeek == Calendar.SUNDAY)
                index = 6;
            else
                index = dayOfWeek - 2;
        } else
            index = dayOfWeek - 1;

        return index;
    }
}