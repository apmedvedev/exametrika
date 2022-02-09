/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.fields;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AggregationRecord} is an aggregation record.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregationRecord implements IAggregationRecord {
    private final IComponentValue value;
    private final long time;
    private final long period;

    public AggregationRecord(IComponentValue value, long time, long period) {
        Assert.notNull(value);

        this.value = value;
        this.time = time;
        this.period = period;
    }

    @Override
    public IComponentValue getValue() {
        return value;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AggregationRecord))
            return false;

        AggregationRecord record = (AggregationRecord) o;
        return value.equals(record.value) && time == record.time && period == record.period;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, time, period);
    }

    @Override
    public String toString() {
        return value.toString() + "@" + time + "[" + period + "]";
    }
}
