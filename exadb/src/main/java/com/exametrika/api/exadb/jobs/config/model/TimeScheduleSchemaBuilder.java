/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link TimeScheduleSchemaBuilder} represents a time schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TimeScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private long startTime;
    private long endTime;
    private boolean included = true;
    private String timeZone;

    public TimeScheduleSchemaBuilder() {
        parent = null;
    }

    public TimeScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public TimeScheduleSchemaBuilder set(int hour) {
        from(hour);
        to(hour);
        return this;
    }

    public TimeScheduleSchemaBuilder set(int hour, int minute) {
        from(hour, minute);
        to(hour, minute);
        return this;
    }

    public TimeScheduleSchemaBuilder set(int hour, int minute, int second) {
        from(hour, minute, second);
        to(hour, minute, second);
        return this;
    }

    public TimeScheduleSchemaBuilder set(int hour, int minute, int second, int millisecond) {
        from(hour, minute, second, millisecond);
        to(hour, minute, second, millisecond);
        return this;
    }

    public TimeScheduleSchemaBuilder set(Calendar calendar) {
        from(calendar);
        to(calendar);
        return this;
    }

    public TimeScheduleSchemaBuilder from(int hour) {
        startTime = time(hour, 0, 0, 0);
        return this;
    }

    public TimeScheduleSchemaBuilder from(int hour, int minute) {
        startTime = time(hour, minute, 0, 0);
        return this;
    }

    public TimeScheduleSchemaBuilder from(int hour, int minute, int second) {
        startTime = time(hour, minute, second, 0);
        return this;
    }

    public TimeScheduleSchemaBuilder from(int hour, int minute, int second, int millisecond) {
        startTime = time(hour, minute, second, millisecond);
        return this;
    }

    public TimeScheduleSchemaBuilder from(Calendar calendar) {
        startTime = calendar.getTimeInMillis();
        return this;
    }

    public TimeScheduleSchemaBuilder to(int hour) {
        endTime = time(hour, 59, 59, 999);
        return this;
    }

    public TimeScheduleSchemaBuilder to(int hour, int minute) {
        endTime = time(hour, minute, 59, 999);
        return this;
    }

    public TimeScheduleSchemaBuilder to(int hour, int minute, int second) {
        endTime = time(hour, minute, second, 999);
        return this;
    }

    public TimeScheduleSchemaBuilder to(int hour, int minute, int second, int millisecond) {
        endTime = time(hour, minute, second, millisecond);
        return this;
    }

    public TimeScheduleSchemaBuilder to(Calendar calendar) {
        boolean minuteSet = calendar.isSet(Calendar.MINUTE);
        boolean secondSet = calendar.isSet(Calendar.SECOND);
        boolean millisecondSet = calendar.isSet(Calendar.MILLISECOND);
        if (!minuteSet)
            calendar.set(Calendar.MINUTE, 59);
        if (!secondSet)
            calendar.set(Calendar.SECOND, 59);
        if (!millisecondSet)
            calendar.set(Calendar.MILLISECOND, 999);

        endTime = calendar.getTimeInMillis();
        return this;
    }

    public TimeScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public TimeScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new TimeScheduleSchemaConfiguration(startTime, endTime, included, timeZone);
    }

    private long time(int hour, int minute, int second, int millisecond) {
        Assert.isTrue(hour >= 0 && hour < 24);
        Assert.isTrue(minute >= 0 && minute < 60);
        Assert.isTrue(second >= 0 && second < 60);
        Assert.isTrue(millisecond >= 0 && millisecond < 1000);

        TimeZone timeZone = this.timeZone != null ? TimeZone.getTimeZone(this.timeZone) : TimeZone.getDefault();
        Calendar current = Calendar.getInstance(timeZone);
        current.clear();

        current.set(Calendar.HOUR_OF_DAY, hour);
        current.set(Calendar.MINUTE, minute);
        current.set(Calendar.SECOND, second);
        current.set(Calendar.MILLISECOND, millisecond);

        return current.getTimeInMillis();
    }
}