/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.StandardSchedulePeriod;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;

/**
 * The {@link StandardSchedulePeriodSchemaConfiguration} represents a configuration of schedule period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardSchedulePeriodSchemaConfiguration extends SchedulePeriodSchemaConfiguration {
    private final UnitType type;
    private final Kind kind;
    private final int amount;
    private final String timeZone;

    public enum UnitType {
        MILLISECOND,
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public enum Kind {
        RELATIVE,
        ABSOLUTE
    }

    public StandardSchedulePeriodSchemaConfiguration(UnitType type, Kind kind, int amount) {
        this(type, kind, amount, null);
    }

    public StandardSchedulePeriodSchemaConfiguration(UnitType type, Kind kind, int amount, String timeZone) {
        Assert.notNull(type);
        Assert.notNull(kind);

        this.type = type;
        this.kind = kind;
        this.amount = amount;
        this.timeZone = timeZone;
    }

    public UnitType getType() {
        return type;
    }

    public Kind getKind() {
        return kind;
    }

    public int getAmount() {
        return amount;
    }

    public long getAbsoluteAmount() {
        switch (type) {
            case MILLISECOND:
                return amount;
            case SECOND:
                return (long) amount * 1000;
            case MINUTE:
                return (long) amount * 60 * 1000;
            case HOUR:
                return (long) amount * 60 * 60 * 1000;
            case DAY:
                return (long) amount * 24 * 60 * 60 * 1000;
            case WEEK:
                return (long) amount * 7 * 24 * 60 * 60 * 1000;
            case MONTH:
                return (long) amount * 4 * 7 * 24 * 60 * 60 * 1000;
            case YEAR:
                return (long) amount * 12 * 4 * 7 * 24 * 60 * 60 * 1000;
            default:
                return Assert.error();
        }
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public ISchedulePeriod createPeriod() {
        return new StandardSchedulePeriod(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardSchedulePeriodSchemaConfiguration))
            return false;

        StandardSchedulePeriodSchemaConfiguration configuration = (StandardSchedulePeriodSchemaConfiguration) o;
        return type == configuration.type && kind == configuration.kind && amount == configuration.amount &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, kind, amount, timeZone);
    }

    @Override
    public String toString() {
        return "[" + kind.toString().toLowerCase() + "]" + Integer.toString(amount) + ":" + type.toString().toLowerCase();
    }
}