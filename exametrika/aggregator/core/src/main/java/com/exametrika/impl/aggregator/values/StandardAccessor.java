/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link StandardAccessor} is an implementation of {@link IFieldAccessor} for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StandardAccessor implements IFieldAccessor {
    private final Type type;

    public enum Type {
        COUNT,
        SUM,
        MIN,
        MAX,
        AVERAGE
    }

    public StandardAccessor(Type type) {
        Assert.notNull(type);

        this.type = type;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IStandardValue value = (IStandardValue) v;
        if (value == null)
            return null;
        if (value.getCount() == 0)
            return null;

        switch (type) {
            case COUNT:
                return value.getCount();
            case SUM:
                return value.getSum();
            case MIN:
                return value.getMin();
            case MAX:
                return value.getMax();
            case AVERAGE:
                return (double) value.getSum() / value.getCount();
            default:
                return Assert.error();
        }
    }
}
