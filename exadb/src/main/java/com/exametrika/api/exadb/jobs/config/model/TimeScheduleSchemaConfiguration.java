/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.TimeSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link TimeScheduleSchemaConfiguration} represents a configuration of time schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final long startTime;
    private final long endTime;
    private final boolean included;
    private final String timeZone;

    public TimeScheduleSchemaConfiguration(long startTime, long endTime, boolean included, String timeZone) {
        Assert.isTrue(startTime <= endTime);

        this.startTime = startTime;
        this.endTime = endTime;
        this.included = included;
        this.timeZone = timeZone;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isIncluded() {
        return included;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public ISchedule createSchedule() {
        return new TimeSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TimeScheduleSchemaConfiguration))
            return false;

        TimeScheduleSchemaConfiguration configuration = (TimeScheduleSchemaConfiguration) o;
        return startTime == configuration.startTime && endTime == configuration.endTime && included == configuration.included &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startTime, endTime, included, timeZone);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        return "time" + (included ? "" : "-") + "(" + format.format(new Date(startTime)) + ".." + format.format(new Date(endTime)) + ")";
    }
}