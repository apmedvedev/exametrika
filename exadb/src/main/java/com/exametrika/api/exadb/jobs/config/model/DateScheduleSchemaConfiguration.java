/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.DateSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link DateScheduleSchemaConfiguration} represents a configuration of date schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DateScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final long startDate;
    private final long endDate;
    private final boolean included;

    public DateScheduleSchemaConfiguration(long startDate, long endDate, boolean included) {
        Assert.isTrue(startDate <= endDate);

        this.startDate = startDate;
        this.endDate = endDate;
        this.included = included;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public boolean isIncluded() {
        return included;
    }

    @Override
    public ISchedule createSchedule() {
        return new DateSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DateScheduleSchemaConfiguration))
            return false;

        DateScheduleSchemaConfiguration configuration = (DateScheduleSchemaConfiguration) o;
        return startDate == configuration.startDate && endDate == configuration.endDate && included == configuration.included;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startDate, endDate, included);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        return "date" + (included ? "" : "-") + "(" + format.format(new Date(startDate)) + ".." + format.format(new Date(endDate)) + ")";
    }
}