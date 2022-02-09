/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.io.Serializable;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.DayOfYearSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link DayOfYearScheduleSchemaConfiguration} represents a configuration of day of year schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfYearScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final DayOfYear startDay;
    private final DayOfYear endDay;
    private final boolean included;
    private final String timeZone;

    public final static class DayOfYear implements Serializable {
        public final int index;
        public final int offset;

        public DayOfYear(int index, int offset) {
            Assert.isTrue((index >= 1 && index <= 366) || index == -1);
            if (index != -1)
                Assert.isTrue(offset == 0);
            Assert.isTrue(offset >= 0 && offset < 366);
            this.index = index;
            this.offset = offset;
        }

        public boolean isLast() {
            return index == -1;
        }

        public int getIndex(int daysInYear) {
            if (index != -1)
                return index;
            else
                return daysInYear - offset;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DayOfYear))
                return false;

            DayOfYear configuration = (DayOfYear) o;
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

    public DayOfYearScheduleSchemaConfiguration(DayOfYear startDay, DayOfYear endDay, boolean included, String timeZone) {
        Assert.notNull(startDay);
        Assert.notNull(endDay);

        this.startDay = startDay;
        this.endDay = endDay;
        this.included = included;
        this.timeZone = timeZone;
    }

    public DayOfYear getStartDay() {
        return startDay;
    }

    public DayOfYear getEndDay() {
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
        return new DayOfYearSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DayOfYearScheduleSchemaConfiguration))
            return false;

        DayOfYearScheduleSchemaConfiguration configuration = (DayOfYearScheduleSchemaConfiguration) o;
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
        return "dayOfYear" + (included ? "" : "-") + "(" + startDay + ".." + endDay + ")";
    }
}