/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.List;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.jobs.schedule.CompositeSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link CompositeScheduleSchemaConfiguration} represents a configuration of composite schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final Type type;
    private final List<ScheduleSchemaConfiguration> schedules;
    private final boolean included;

    public enum Type {
        OR,
        AND
    }

    public CompositeScheduleSchemaConfiguration(Type type, List<ScheduleSchemaConfiguration> schedules, boolean included) {
        Assert.notNull(type);
        Assert.notNull(schedules);

        this.type = type;
        this.schedules = Immutables.wrap(schedules);
        this.included = included;
    }

    public Type getType() {
        return type;
    }

    public List<ScheduleSchemaConfiguration> getSchedules() {
        return schedules;
    }

    public boolean isIncluded() {
        return included;
    }

    @Override
    public ISchedule createSchedule() {
        return new CompositeSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeScheduleSchemaConfiguration))
            return false;

        CompositeScheduleSchemaConfiguration configuration = (CompositeScheduleSchemaConfiguration) o;
        return type == configuration.type && schedules.equals(configuration.schedules) && included == configuration.included;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, schedules, included);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        if (type == Type.AND)
            return "and" + (included ? "" : "-") + "\n(\n" + toString(schedules, locale) + "\n)";
        else
            return "or" + (included ? "" : "-") + "\n(\n" + toString(schedules, locale) + "\n)";
    }

    private String toString(List<ScheduleSchemaConfiguration> collection, Locale locale) {
        Assert.notNull(collection);

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (ScheduleSchemaConfiguration element : collection) {
            if (first)
                first = false;
            else
                builder.append('\n');

            builder.append(element.toString(locale));
        }

        return Strings.indent(builder.toString(), 4).toString();
    }
}