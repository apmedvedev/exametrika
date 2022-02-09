/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.TimeScheduleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link TimeSchedule} represents a time schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeSchedule implements ISchedule {
    private final TimeScheduleSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public TimeSchedule(TimeScheduleSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long value) {
        Calendar current = Calendar.getInstance(timeZone);
        Calendar time = Calendar.getInstance(timeZone);
        time.clear();

        current.setTimeInMillis(value);
        time.set(Calendar.HOUR_OF_DAY, current.get(Calendar.HOUR_OF_DAY));
        time.set(Calendar.MINUTE, current.get(Calendar.MINUTE));
        time.set(Calendar.SECOND, current.get(Calendar.SECOND));
        time.set(Calendar.MILLISECOND, current.get(Calendar.MILLISECOND));

        long timeValue = time.getTimeInMillis();
        boolean res = timeValue >= configuration.getStartTime() && timeValue <= configuration.getEndTime();
        if (configuration.isIncluded())
            return res;
        else
            return !res;
    }
}