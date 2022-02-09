/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.io.Serializable;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.DayOfWeekInMonthSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link DayOfWeekInMonthScheduleSchemaConfiguration} represents a configuration of day of week in month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfWeekInMonthScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final DayOfWeekInMonth startDay;
    private final DayOfWeekInMonth endDay;
    private final boolean included;
    private final String timeZone;

    public final static class DayOfWeekInMonth implements Serializable {
        public final int dayOfWeekIndex;
        public final int dayOfWeekInMonthIndex;
        public final int offset;

        public DayOfWeekInMonth(int dayOfWeekIndex, int dayOfWeekInMonthIndex, int offset) {
            Assert.isTrue(dayOfWeekIndex >= 0 && dayOfWeekIndex < 7);
            Assert.isTrue((dayOfWeekInMonthIndex >= 1 && dayOfWeekInMonthIndex <= 5) || dayOfWeekInMonthIndex == -1);
            if (dayOfWeekInMonthIndex != -1)
                Assert.isTrue(offset == 0);
            Assert.isTrue(offset >= 0 && offset < 5);

            this.dayOfWeekIndex = dayOfWeekIndex;
            this.dayOfWeekInMonthIndex = dayOfWeekInMonthIndex;
            this.offset = offset;
        }

        public boolean isLast() {
            return dayOfWeekInMonthIndex == -1;
        }

        public int getDayOfWeekInMonthIndex(int daysOfWeekInMonth) {
            if (dayOfWeekInMonthIndex != -1)
                return dayOfWeekInMonthIndex;
            else
                return daysOfWeekInMonth - offset;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DayOfWeekInMonth))
                return false;

            DayOfWeekInMonth configuration = (DayOfWeekInMonth) o;
            return dayOfWeekIndex == configuration.dayOfWeekIndex &&
                    dayOfWeekInMonthIndex == configuration.dayOfWeekInMonthIndex && offset == configuration.offset;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(dayOfWeekIndex, dayOfWeekInMonthIndex, offset);
        }

        @Override
        public String toString() {
            if (dayOfWeekInMonthIndex != -1)
                return Integer.toString(dayOfWeekIndex) + "/" + Integer.toString(dayOfWeekInMonthIndex);
            else if (offset == 0)
                return Integer.toString(dayOfWeekIndex) + "/" + "*";
            else
                return Integer.toString(dayOfWeekIndex) + "/" + "*-" + Integer.toString(offset);
        }
    }

    public DayOfWeekInMonthScheduleSchemaConfiguration(DayOfWeekInMonth startDay, DayOfWeekInMonth endDay, boolean included, String timeZone) {
        Assert.notNull(startDay);
        Assert.notNull(endDay);
        this.startDay = startDay;
        this.endDay = endDay;
        this.included = included;
        this.timeZone = timeZone;
    }

    public DayOfWeekInMonth getStartDay() {
        return startDay;
    }

    public DayOfWeekInMonth getEndDay() {
        return endDay;
    }

    public boolean isIncluded() {
        return included;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public ISchedule createSchedule() {
        return new DayOfWeekInMonthSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DayOfWeekInMonthScheduleSchemaConfiguration))
            return false;

        DayOfWeekInMonthScheduleSchemaConfiguration configuration = (DayOfWeekInMonthScheduleSchemaConfiguration) o;
        return startDay.equals(configuration.startDay) && endDay.equals(configuration.endDay) && included == configuration.included &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startDay, endDay, included, timeZone);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        return "dayOfWeekInMonth" + (included ? "" : "-") + "(" + startDay + ".." + endDay + ")";
    }
}