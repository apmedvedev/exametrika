/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.io.Serializable;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.DayOfMonthSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link DayOfMonthScheduleSchemaConfiguration} represents a configuration of day of month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfMonthScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final DayOfMonth startDay;
    private final DayOfMonth endDay;
    private final boolean included;
    private final String timeZone;

    public final static class DayOfMonth implements Serializable {
        public final int index;
        public final int offset;

        public DayOfMonth(int index, int offset) {
            Assert.isTrue((index >= 1 && index <= 31) || index == -1);
            if (index != -1)
                Assert.isTrue(offset == 0);
            Assert.isTrue(offset >= 0 && offset < 31);
            this.index = index;
            this.offset = offset;
        }

        public boolean isLast() {
            return index == -1;
        }

        public int getIndex(int daysInMonth) {
            if (index != -1)
                return index;
            else
                return daysInMonth - offset;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DayOfMonth))
                return false;

            DayOfMonth configuration = (DayOfMonth) o;
            return index == configuration.index && offset == configuration.offset;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(index, offset);
        }

        @Override
        public String toString() {
            if (index != -1)
                return Integer.toString(index);
            else if (offset == 0)
                return "*";
            else
                return "*-" + Integer.toString(offset);
        }
    }

    public DayOfMonthScheduleSchemaConfiguration(DayOfMonth startDay, DayOfMonth endDay, boolean included, String timeZone) {
        Assert.notNull(startDay);
        Assert.notNull(endDay);

        this.startDay = startDay;
        this.endDay = endDay;
        this.included = included;
        this.timeZone = timeZone;
    }

    public DayOfMonth getStartDay() {
        return startDay;
    }

    public DayOfMonth getEndDay() {
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
        return new DayOfMonthSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DayOfMonthScheduleSchemaConfiguration))
            return false;

        DayOfMonthScheduleSchemaConfiguration configuration = (DayOfMonthScheduleSchemaConfiguration) o;
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
        return "dayOfMonth" + (included ? "" : "-") + "(" + startDay + ".." + endDay + ")";
    }
}