/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.api.exadb.jobs.config.model.DayOfYearScheduleSchemaConfiguration.DayOfYear;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link DayOfYearScheduleSchemaBuilder} represents a day of year schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DayOfYearScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private DayOfYear startDay;
    private DayOfYear endDay;
    private boolean included = true;
    private String timeZone;

    public DayOfYearScheduleSchemaBuilder() {
        parent = null;
    }

    public DayOfYearScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public DayOfYearScheduleSchemaBuilder set(int day) {
        from(day);
        to(day);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder last() {
        fromLast();
        toLast();
        return this;
    }

    public DayOfYearScheduleSchemaBuilder last(int offset) {
        fromLast(offset);
        toLast(offset);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder from(int day) {
        startDay = new DayOfYear(day, 0);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder fromLast() {
        startDay = new DayOfYear(-1, 0);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder fromLast(int offset) {
        startDay = new DayOfYear(-1, offset);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder to(int day) {
        endDay = new DayOfYear(day, 0);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder toLast() {
        endDay = new DayOfYear(-1, 0);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder toLast(int offset) {
        endDay = new DayOfYear(-1, offset);
        return this;
    }

    public DayOfYearScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public DayOfYearScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new DayOfYearScheduleSchemaConfiguration(startDay, endDay, included, timeZone);
    }
}