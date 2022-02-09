/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.List;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.DayOfWeekSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link DayOfWeekScheduleSchemaConfiguration} represents a configuration of day of week schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DayOfWeekScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final List<Boolean> weekDays;
    private final boolean included;
    private final String timeZone;

    public DayOfWeekScheduleSchemaConfiguration(List<Boolean> weekDays, boolean included, String timeZone) {
        Assert.notNull(weekDays);
        Assert.isTrue(weekDays.size() == 7);

        this.weekDays = Immutables.wrap(weekDays);
        this.included = included;
        this.timeZone = timeZone;
    }

    public List<Boolean> getWeekDays() {
        return weekDays;
    }

    public boolean isIncluded() {
        return included;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public ISchedule createSchedule() {
        return new DayOfWeekSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DayOfWeekScheduleSchemaConfiguration))
            return false;

        DayOfWeekScheduleSchemaConfiguration configuration = (DayOfWeekScheduleSchemaConfiguration) o;
        return weekDays.equals(configuration.weekDays) && included == configuration.included &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(weekDays, included, timeZone);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < weekDays.size(); i++) {
            if (!weekDays.get(i))
                continue;

            if (first)
                first = false;
            else
                builder.append(',');

            builder.append(i + 1);
        }

        return "dayOfWeek" + (included ? "" : "-") + "(" + builder.toString() + ")";
    }
}