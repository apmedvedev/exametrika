/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.List;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.MonthSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link MonthScheduleSchemaConfiguration} represents a configuration of month schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonthScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final List<Boolean> months;
    private final boolean included;
    private final String timeZone;

    public MonthScheduleSchemaConfiguration(List<Boolean> months, boolean included, String timeZone) {
        Assert.notNull(months);
        Assert.isTrue(months.size() == 12);

        this.months = Immutables.wrap(months);
        this.included = included;
        this.timeZone = timeZone;
    }

    public List<Boolean> getMonths() {
        return months;
    }

    public boolean isIncluded() {
        return included;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public ISchedule createSchedule() {
        return new MonthSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MonthScheduleSchemaConfiguration))
            return false;

        MonthScheduleSchemaConfiguration configuration = (MonthScheduleSchemaConfiguration) o;
        return months.equals(configuration.months) && included == configuration.included &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(months, included, timeZone);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < months.size(); i++) {
            if (!months.get(i))
                continue;

            if (first)
                first = false;
            else
                builder.append(',');

            builder.append(i + 1);
        }

        return "month" + (included ? "" : "-") + "(" + builder.toString() + ")";
    }
}