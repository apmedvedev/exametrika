/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.api.exadb.jobs.config.model.DayOfMonthScheduleSchemaConfiguration.DayOfMonth;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link DayOfMonthScheduleSchemaBuilder} represents a day of month schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DayOfMonthScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private DayOfMonth startDay;
    private DayOfMonth endDay;
    private boolean included = true;
    private String timeZone;

    public DayOfMonthScheduleSchemaBuilder() {
        parent = null;
    }

    public DayOfMonthScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public DayOfMonthScheduleSchemaBuilder set(int day) {
        from(day);
        to(day);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder last() {
        fromLast();
        toLast();
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder last(int offset) {
        fromLast(offset);
        toLast(offset);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder from(int day) {
        startDay = new DayOfMonth(day, 0);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder fromLast() {
        startDay = new DayOfMonth(-1, 0);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder fromLast(int offset) {
        startDay = new DayOfMonth(-1, offset);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder to(int day) {
        endDay = new DayOfMonth(day, 0);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder toLast() {
        endDay = new DayOfMonth(-1, 0);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder toLast(int offset) {
        endDay = new DayOfMonth(-1, offset);
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public DayOfMonthScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new DayOfMonthScheduleSchemaConfiguration(startDay, endDay, included, timeZone);
    }
}