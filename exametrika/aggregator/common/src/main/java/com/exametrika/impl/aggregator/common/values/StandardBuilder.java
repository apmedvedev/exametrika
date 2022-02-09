/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StandardBuilder} is a measurement data for standard long fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StandardBuilder implements IFieldValueBuilder, IStandardValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(StandardBuilder.class);
    private long count;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private long sum;

    public StandardBuilder() {
    }

    public StandardBuilder(long count, long min, long max, long sum) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.sum = sum;
    }

    @Override
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public long getMin() {
        return min;
    }

    public void setMin(long value) {
        min = value;
    }

    @Override
    public long getMax() {
        return max;
    }

    public void setMax(long value) {
        max = value;
    }

    @Override
    public long getSum() {
        return sum;
    }

    public void setSum(long value) {
        sum = value;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IFieldValue value) {
        Assert.notNull(value);

        IStandardValue standardValue = (IStandardValue) value;

        count = standardValue.getCount();
        min = standardValue.getMin();
        max = standardValue.getMax();
        sum = standardValue.getSum();
    }

    @Override
    public IFieldValue toValue() {
        return new StandardValue(count, min, max, sum);
    }

    @Override
    public void clear() {
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
        this.sum = 0;
        this.count = 0;
    }

    @Override
    public void normalizeEnd(long count) {
        sum = sum / count;
        this.count = 1;
    }

    @Override
    public void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum) {
        count = 1;
        min = sum;
        max = sum;
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardBuilder))
            return false;

        StandardBuilder data = (StandardBuilder) o;
        return count == data.count && min == data.min && max == data.max && sum == data.sum;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(count, min, max, sum);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
