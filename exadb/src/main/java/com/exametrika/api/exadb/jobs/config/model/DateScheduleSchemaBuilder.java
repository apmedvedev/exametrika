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
 * The {@link DateScheduleSchemaBuilder} represents a date schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DateScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private long startDate;
    private long endDate;
    private boolean included = true;
    private String timeZone;

    public DateScheduleSchemaBuilder() {
        parent = null;
    }

    public DateScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public DateScheduleSchemaBuilder set(int year) {
        from(year);
        to(year);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month) {
        from(year, month);
        to(year, month);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month, int day) {
        from(year, month, day);
        to(year, month, day);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month, int day, int hour) {
        from(year, month, day, hour);
        to(year, month, day, hour);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month, int day, int hour, int minute) {
        from(year, month, day, hour, minute);
        to(year, month, day, hour, minute);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month, int day, int hour, int minute, int second) {
        from(year, month, day, hour, minute, second);
        to(year, month, day, hour, minute, second);
        return this;
    }

    public DateScheduleSchemaBuilder set(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        from(year, month, day, hour, minute, second, millisecond);
        to(year, month, day, hour, minute, second, millisecond);
        return this;
    }

    public DateScheduleSchemaBuilder set(Calendar calendar) {
        from(calendar);
        to(calendar);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year) {
        startDate = date(year, 0, 1, 0, 0, 0, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month) {
        startDate = date(year, month, 1, 0, 0, 0, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month, int day) {
        startDate = date(year, month, day, 0, 0, 0, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month, int day, int hour) {
        startDate = date(year, month, day, hour, 0, 0, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month, int day, int hour, int minute) {
        startDate = date(year, month, day, hour, minute, 0, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month, int day, int hour, int minute, int second) {
        startDate = date(year, month, day, hour, minute, second, 0);
        return this;
    }

    public DateScheduleSchemaBuilder from(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        startDate = date(year, month, day, hour, minute, second, millisecond);
        return this;
    }

    public DateScheduleSchemaBuilder from(Calendar calendar) {
        startDate = calendar.getTimeInMillis();
        return this;
    }

    public DateScheduleSchemaBuilder to(int year) {
        endDate = date(year, 12, 31, 23, 59, 59, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month) {
        endDate = date(year, month, -1, 23, 59, 59, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month, int day) {
        endDate = date(year, month, day, 23, 59, 59, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month, int day, int hour) {
        endDate = date(year, month, day, hour, 59, 59, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month, int day, int hour, int minute) {
        endDate = date(year, month, day, hour, minute, 59, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month, int day, int hour, int minute, int second) {
        endDate = date(year, month, day, hour, minute, second, 999);
        return this;
    }

    public DateScheduleSchemaBuilder to(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        endDate = date(year, month, day, hour, minute, second, millisecond);
        return this;
    }

    public DateScheduleSchemaBuilder to(Calendar calendar) {
        boolean monthSet = calendar.isSet(Calendar.MONTH);
        boolean dayOfMonthSet = calendar.isSet(Calendar.DAY_OF_MONTH);
        boolean hourSet = calendar.isSet(Calendar.HOUR_OF_DAY);
        boolean minuteSet = calendar.isSet(Calendar.MINUTE);
        boolean secondSet = calendar.isSet(Calendar.SECOND);
        boolean millisecondSet = calendar.isSet(Calendar.MILLISECOND);

        if (!monthSet)
            calendar.set(Calendar.MONTH, 11);
        if (!dayOfMonthSet) {
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            calendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
        if (!hourSet)
            calendar.set(Calendar.HOUR_OF_DAY, 23);
        if (!minuteSet)
            calendar.set(Calendar.MINUTE, 59);
        if (!secondSet)
            calendar.set(Calendar.SECOND, 59);
        if (!millisecondSet)
            calendar.set(Calendar.MILLISECOND, 999);

        endDate = calendar.getTimeInMillis();
        return this;
    }

    public DateScheduleSchemaBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public DateScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new DateScheduleSchemaConfiguration(startDate, endDate, included);
    }

    private long date(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Assert.isTrue(year >= 0);
        Assert.isTrue(month >= 1 && month <= 12);
        Assert.isTrue((day >= 1 && day <= 31) || day == -1);
        Assert.isTrue(hour >= 0 && hour < 24);
        Assert.isTrue(minute >= 0 && minute < 60);
        Assert.isTrue(second >= 0 && second < 60);
        Assert.isTrue(millisecond >= 0 && millisecond < 1000);

        TimeZone timeZone = this.timeZone != null ? TimeZone.getTimeZone(this.timeZone) : TimeZone.getDefault();
        Calendar current = Calendar.getInstance(timeZone);
        current.clear();
        current.set(Calendar.YEAR, year);
        current.set(Calendar.MONTH, month - 1);
        if (day != -1)
            current.set(Calendar.DAY_OF_MONTH, day);
        else {
            int daysInMonth = current.getActualMaximum(Calendar.DAY_OF_MONTH);
            current.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
        current.set(Calendar.HOUR_OF_DAY, hour);
        current.set(Calendar.MINUTE, minute);
        current.set(Calendar.SECOND, second);
        current.set(Calendar.MILLISECOND, millisecond);

        return current.getTimeInMillis();
    }
}