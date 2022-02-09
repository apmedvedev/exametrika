/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link RateComputer} is an implementation of {@link IFieldComputer} for rate fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RateComputer implements IFieldComputer {
    private final IMetricAccessor baseFieldAccessor;

    public RateComputer(IMetricAccessor baseFieldAccessor) {
        Assert.notNull(baseFieldAccessor);

        this.baseFieldAccessor = baseFieldAccessor;
    }

    public Object getRate(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        Object baseValue = baseFieldAccessor.get(componentValue, metricValue, context);

        long period = context.getPeriod();
        if (period == 0)
            return null;

        if (baseValue instanceof Number)
            return ((Number) baseValue).doubleValue() * 1000 / period;
        else if (baseValue instanceof JsonArray) {
            JsonArrayBuilder builder = new JsonArrayBuilder();
            for (Object element : (JsonArray) baseValue) {
                if (element instanceof Number)
                    builder.add(((Number) element).doubleValue() * 1000 / period);
                else
                    return null;
            }

            return builder.toJson();
        } else
            return null;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        return getRate(componentValue, metricValue, context);
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }
}