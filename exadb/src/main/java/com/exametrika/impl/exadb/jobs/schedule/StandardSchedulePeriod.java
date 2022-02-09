/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.util.Calendar;
import java.util.TimeZone;

import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;

/**
 * The {@link StandardSchedulePeriod} represents a schedule period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardSchedulePeriod implements ISchedulePeriod {
    private final StandardSchedulePeriodSchemaConfiguration configuration;
    private final TimeZone timeZone;

    public StandardSchedulePeriod(StandardSchedulePeriodSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
    }

    @Override
    public boolean evaluate(long startTime, long endTime) {
        Calendar current;

        if (configuration.getKind() == Kind.ABSOLUTE) {
            Calendar base = Calendar.getInstance(timeZone);
            base.setTimeInMillis(startTime);
            current = Calendar.getInstance(timeZone);
            current.clear();
            setFields(base, current);
        } else if (configuration.getKind() == Kind.RELATIVE) {
            current = Calendar.getInstance(timeZone);
            current.setTimeInMillis(startTime);
        } else
            return Assert.error();

        int field;
        switch (configuration.getType()) {
            case MILLISECOND:
                field = Calendar.MILLISECOND;
                break;
            case SECOND:
                field = Calendar.SECOND;
                break;
            case MINUTE:
                field = Calendar.MINUTE;
                break;
            case HOUR:
                field = Calendar.HOUR;
                break;
            case DAY:
                field = Calendar.DAY_OF_YEAR;
                break;
            case WEEK:
                field = Calendar.WEEK_OF_YEAR;
                break;
            case MONTH:
                field = Calendar.MONTH;
                break;
            case YEAR:
                field = Calendar.YEAR;
                break;
            default:
                return Assert.error();
        }

        current.add(field, configuration.getAmount());

        if (current.getTimeInMillis() <= endTime)
            return true;
        else
            return false;
    }

    private void setFields(Calendar base, Calendar current) {
        boolean rounded = false;
        switch (configuration.getType()) {
            case MILLISECOND:
                current.set(Calendar.MILLISECOND, floor(base.get(Calendar.MILLISECOND), rounded));
                rounded = true;
            case SECOND:
                current.set(Calendar.SECOND, floor(base.get(Calendar.SECOND), rounded));
                rounded = true;
            case MINUTE:
                current.set(Calendar.MINUTE, floor(base.get(Calendar.MINUTE), rounded));
                rounded = true;
            case HOUR:
                current.set(Calendar.HOUR_OF_DAY, floor(base.get(Calendar.HOUR_OF_DAY), rounded));
                rounded = true;
            case DAY:
                current.set(Calendar.DAY_OF_MONTH, floor(base.get(Calendar.DAY_OF_MONTH), rounded));
                rounded = true;
            case WEEK:
                current.set(Calendar.WEEK_OF_YEAR, floor(base.get(Calendar.WEEK_OF_YEAR), rounded));
                rounded = true;
            case MONTH:
                current.set(Calendar.MONTH, floor(base.get(Calendar.MONTH), rounded));
                rounded = true;
            case YEAR:
                current.set(Calendar.YEAR, floor(base.get(Calendar.YEAR), rounded));
                rounded = true;
                break;
            default:
                Assert.error();
        }
    }

    private int floor(int value, boolean rounded) {
        if (rounded)
            return value;

        int amount = configuration.getAmount();
        return value / amount * amount;
    }
}