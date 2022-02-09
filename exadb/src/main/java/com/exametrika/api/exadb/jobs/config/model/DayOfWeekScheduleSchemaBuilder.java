/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link DayOfWeekScheduleSchemaBuilder} represents a day of week schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DayOfWeekScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private boolean[] weekDays = new boolean[7];
    private boolean included = true;
    private String timeZone;

    public DayOfWeekScheduleSchemaBuilder() {
        parent = null;
    }

    public DayOfWeekScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public DayOfWeekScheduleSchemaBuilder set(int weekDay) {
        Assert.isTrue(weekDay >= 1 && weekDay <= 7);
        weekDays[weekDay - 1] = true;
        return this;
    }

    public DayOfWeekScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public DayOfWeekScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        List<Boolean> list = new ArrayList<Boolean>();
        for (int i = 0; i < 7; i++)
            list.add(weekDays[i]);
        return new DayOfWeekScheduleSchemaConfiguration(list, included, timeZone);
    }
}