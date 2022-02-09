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
 * The {@link MonthScheduleSchemaBuilder} represents a month schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MonthScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private boolean[] months = new boolean[12];
    private boolean included = true;
    private String timeZone;

    public MonthScheduleSchemaBuilder() {
        parent = null;
    }

    public MonthScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public MonthScheduleSchemaBuilder set(int month) {
        Assert.isTrue(month >= 1 && month <= 12);
        months[month - 1] = true;
        return this;
    }

    public MonthScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public MonthScheduleSchemaBuilder exclude() {
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
        for (int i = 0; i < 12; i++)
            list.add(months[i]);
        return new MonthScheduleSchemaConfiguration(list, included, timeZone);
    }
}