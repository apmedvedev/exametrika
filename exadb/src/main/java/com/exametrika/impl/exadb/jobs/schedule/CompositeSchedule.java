/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link CompositeSchedule} represents a composite schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeSchedule implements ISchedule {
    private final CompositeScheduleSchemaConfiguration configuration;
    private final List<ISchedule> schedules;

    public CompositeSchedule(CompositeScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;

        List<ISchedule> schedules = new ArrayList<ISchedule>();
        for (ScheduleSchemaConfiguration schedule : configuration.getSchedules())
            schedules.add(schedule.createSchedule());

        this.schedules = schedules;
    }

    @Override
    public boolean evaluate(long value) {
        boolean res;
        switch (configuration.getType()) {
            case AND:
                res = true;
                for (ISchedule schedule : schedules) {
                    if (!schedule.evaluate(value)) {
                        res = false;
                        break;
                    }
                }
                break;
            case OR:
                res = false;
                for (ISchedule schedule : schedules) {
                    if (schedule.evaluate(value)) {
                        res = true;
                        break;
                    }
                }
                break;
            default:
                return Assert.error();
        }

        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}