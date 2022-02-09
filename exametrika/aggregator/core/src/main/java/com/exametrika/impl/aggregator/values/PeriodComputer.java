/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link PeriodComputer} is an implementation of {@link IFieldComputer} for period fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodComputer implements IFieldComputer {
    private final IMetricAccessor baseFieldAccessor;
    private final IMetricAccessor prevPeriodAccessor;

    public PeriodComputer(IMetricAccessor baseFieldAccessor, IMetricAccessor prevPeriodAccessor) {
        Assert.notNull(baseFieldAccessor);
        Assert.notNull(prevPeriodAccessor);

        this.baseFieldAccessor = baseFieldAccessor;
        this.prevPeriodAccessor = prevPeriodAccessor;
    }

    public Object getDelta(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context, boolean percentage) {
        Object currentPeriodValue = baseFieldAccessor.get(componentValue, metricValue, context);
        Object prevPeriodValue = prevPeriodAccessor.get(componentValue, metricValue, context);

        return getDelta(currentPeriodValue, prevPeriodValue, percentage);
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        Object currentPeriodValue = baseFieldAccessor.get(componentValue, metricValue, context);
        Object prevPeriodValue = prevPeriodAccessor.get(componentValue, metricValue, context);

        Object delta = getDelta(currentPeriodValue, prevPeriodValue, false);
        Object deltaPercentage = getDelta(currentPeriodValue, prevPeriodValue, true);

        if (delta == null || deltaPercentage == null)
            return null;

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("delta", delta);
        fields.put("delta%", deltaPercentage);

        return fields.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }

    private Object getDelta(Object currentPeriodValue, Object prevPeriodValue, boolean percentage) {
        if (currentPeriodValue instanceof JsonArray && prevPeriodValue instanceof JsonArray) {
            JsonArray current = (JsonArray) currentPeriodValue;
            JsonArray prev = (JsonArray) prevPeriodValue;
            if (current.size() != prev.size())
                return null;

            JsonArrayBuilder deltaBuilder = new JsonArrayBuilder();
            for (int i = 0; i < current.size(); i++) {
                Object currentElement = current.get(i);
                Object prevElement = prev.get(i);

                Object delta = getDelta(currentElement, prevElement, percentage);
                if (delta == null)
                    return null;

                deltaBuilder.add(delta);
            }

            return deltaBuilder.toJson();
        } else if (currentPeriodValue instanceof Double && prevPeriodValue instanceof Double) {
            double current = (Double) currentPeriodValue;
            double prev = (Double) prevPeriodValue;

            if (!percentage)
                return current - prev;
            else
                return Numbers.percents(current - prev, prev);
        } else if (currentPeriodValue instanceof Number && prevPeriodValue instanceof Number) {
            long current = ((Number) currentPeriodValue).longValue();
            long prev = ((Number) prevPeriodValue).longValue();

            if (!percentage)
                return current - prev;
            else
                return Numbers.percents(current - prev, prev);
        } else
            return null;
    }
}