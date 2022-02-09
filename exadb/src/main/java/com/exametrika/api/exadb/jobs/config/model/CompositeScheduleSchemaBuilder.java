/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaConfiguration.Type;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link CompositeScheduleSchemaBuilder} represents a composite schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CompositeScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private final Type type;
    private List<ScheduleSchemaBuilder> children = new ArrayList<ScheduleSchemaBuilder>();
    private boolean included = true;
    private String timeZone;

    public CompositeScheduleSchemaBuilder(CompositeScheduleSchemaConfiguration.Type type) {
        Assert.notNull(type);

        this.parent = null;
        this.type = type;
    }

    public CompositeScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent, CompositeScheduleSchemaConfiguration.Type type) {
        Assert.notNull(type);

        this.parent = parent;
        this.type = type;
    }

    public CompositeScheduleSchemaBuilder andGroup() {
        CompositeScheduleSchemaBuilder builder = new CompositeScheduleSchemaBuilder(this, CompositeScheduleSchemaConfiguration.Type.AND);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public CompositeScheduleSchemaBuilder orGroup() {
        CompositeScheduleSchemaBuilder builder = new CompositeScheduleSchemaBuilder(this, CompositeScheduleSchemaConfiguration.Type.OR);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public TimeScheduleSchemaBuilder time() {
        TimeScheduleSchemaBuilder builder = new TimeScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public DateScheduleSchemaBuilder date() {
        DateScheduleSchemaBuilder builder = new DateScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public DayOfMonthScheduleSchemaBuilder dayOfMonth() {
        DayOfMonthScheduleSchemaBuilder builder = new DayOfMonthScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder dayOfWeekInMonth() {
        DayOfWeekInMonthScheduleSchemaBuilder builder = new DayOfWeekInMonthScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public DayOfWeekScheduleSchemaBuilder dayOfWeek() {
        DayOfWeekScheduleSchemaBuilder builder = new DayOfWeekScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public DayOfYearScheduleSchemaBuilder dayOfYear() {
        DayOfYearScheduleSchemaBuilder builder = new DayOfYearScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public MonthScheduleSchemaBuilder month() {
        MonthScheduleSchemaBuilder builder = new MonthScheduleSchemaBuilder(this);
        builder.timeZone(timeZone);
        children.add(builder);

        return builder;
    }

    public LowMemoryScheduleSchemaBuilder lowMemory() {
        LowMemoryScheduleSchemaBuilder builder = new LowMemoryScheduleSchemaBuilder(this);
        children.add(builder);

        return builder;
    }

    public LowDiskScheduleSchemaBuilder lowDisk() {
        LowDiskScheduleSchemaBuilder builder = new LowDiskScheduleSchemaBuilder(this);
        children.add(builder);

        return builder;
    }

    public CompositeScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public CompositeScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        List<ScheduleSchemaConfiguration> list = new ArrayList<ScheduleSchemaConfiguration>();
        for (ScheduleSchemaBuilder child : children)
            list.add(child.toSchedule());

        return new CompositeScheduleSchemaConfiguration(type, list, included);
    }
}