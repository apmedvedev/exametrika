/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Objects;


/**
 * The {@link StandardValue} is a measurement data for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardValue implements IStandardValue {
    private final long count;
    private final long min;
    private final long max;
    private final long sum;

    public StandardValue(long count, long min, long max, long sum) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.sum = sum;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public long getMin() {
        return min;
    }

    @Override
    public long getMax() {
        return max;
    }

    @Override
    public long getSum() {
        return sum;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();

        fields.put("instanceOf", "std");
        fields.put("count", count);
        fields.put("sum", sum);

        if (min != Long.MAX_VALUE) {
            fields.put("min", min);
            fields.put("max", max);
        }

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardValue))
            return false;

        StandardValue data = (StandardValue) o;
        return count == data.count && min == data.min && max == data.max && sum == data.sum;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(count, min, max, sum);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
