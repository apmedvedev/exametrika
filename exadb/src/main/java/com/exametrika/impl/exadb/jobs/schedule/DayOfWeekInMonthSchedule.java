/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.DayOfWeekInMonthScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link DayOfWeekInMonthSchedule} represents a day of week in month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfWeekInMonthSchedule implements ISchedule {
    private final DayOfWeekInMonthScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public DayOfWeekInMonthSchedule(DayOfWeekInMonthScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);

        current.setTimeInMillis(value);
        int day = current.get(Calendar.DAY_OF_MONTH);

        current.set(Calendar.DAY_OF_WEEK, getDayOfWeek(current, configuration.getStartDay().dayOfWeekIndex));
        int daysOfWeekInMonth = current.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);
        int startDayOfWeekInMonth = configuration.getStartDay().getDayOfWeekInMonthIndex(daysOfWeekInMonth);
        current.set(Calendar.DAY_OF_WEEK_IN_MONTH, startDayOfWeekInMonth);
        int startDay = current.get(Calendar.DAY_OF_MONTH);

        current.set(Calendar.DAY_OF_WEEK, getDayOfWeek(current, configuration.getEndDay().dayOfWeekIndex));
        daysOfWeekInMonth = current.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);
        int endDayOfWeekInMonth = configuration.getEndDay().getDayOfWeekInMonthIndex(daysOfWeekInMonth);
        current.set(Calendar.DAY_OF_WEEK_IN_MONTH, endDayOfWeekInMonth);
        int endDay = current.get(Calendar.DAY_OF_MONTH);

        boolean res = day >= startDay && day <= endDay;
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }

    private int getDayOfWeek(Calendar calendar, int dayOfWeek) {
        if (calendar.getFirstDayOfWeek() == Calendar.MONDAY) {
            if (dayOfWeek == 7)
                return 1;
            else
                return dayOfWeek + 1;
        } else
            return dayOfWeek;
    }
}