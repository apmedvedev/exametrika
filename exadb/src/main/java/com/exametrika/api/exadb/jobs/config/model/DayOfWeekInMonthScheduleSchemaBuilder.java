/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.api.exadb.jobs.config.model.DayOfWeekInMonthScheduleSchemaConfiguration.DayOfWeekInMonth;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link DayOfWeekInMonthScheduleSchemaBuilder} represents a day of week in month schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DayOfWeekInMonthScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private DayOfWeekInMonth startDay;
    private DayOfWeekInMonth endDay;
    private boolean included = true;
    private String timeZone;

    public DayOfWeekInMonthScheduleSchemaBuilder() {
        parent = null;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder set(int dayOfWeek, int dayOfWeekInMonth) {
        from(dayOfWeek, dayOfWeekInMonth);
        to(dayOfWeek, dayOfWeekInMonth);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder last(int dayOfWeek) {
        fromLast(dayOfWeek);
        toLast(dayOfWeek);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder last(int dayOfWeek, int offset) {
        fromLast(dayOfWeek, offset);
        toLast(dayOfWeek, offset);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder from(int dayOfWeek, int dayOfWeekInMonth) {
        startDay = new DayOfWeekInMonth(dayOfWeek, dayOfWeekInMonth, 0);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder fromLast(int dayOfWeek) {
        startDay = new DayOfWeekInMonth(dayOfWeek, -1, 0);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder fromLast(int dayOfWeek, int offset) {
        startDay = new DayOfWeekInMonth(dayOfWeek, -1, offset);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder to(int dayOfWeek, int dayOfWeekInMonth) {
        endDay = new DayOfWeekInMonth(dayOfWeek, dayOfWeekInMonth, 0);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder toLast(int dayOfWeek) {
        endDay = new DayOfWeekInMonth(dayOfWeek, -1, 0);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder toLast(int dayOfWeek, int offset) {
        endDay = new DayOfWeekInMonth(dayOfWeek, -1, offset);
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public DayOfWeekInMonthScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new DayOfWeekInMonthScheduleSchemaConfiguration(startDay, endDay, included, timeZone);
    }
}