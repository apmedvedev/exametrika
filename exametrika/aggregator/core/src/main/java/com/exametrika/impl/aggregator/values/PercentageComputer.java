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
import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link PercentageComputer} is an implementation of {@link IFieldComputer} for percentage fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PercentageComputer implements IFieldComputer {
    private final IMetricAccessor currentAccessor;
    private final IMetricAccessor baseAccessor;
    private final String nodeType;

    public PercentageComputer(IMetricAccessor currentAccessor, IMetricAccessor baseAccessor, String nodeType) {
        Assert.notNull(currentAccessor);
        Assert.notNull(baseAccessor);

        this.currentAccessor = currentAccessor;
        this.baseAccessor = baseAccessor;
        this.nodeType = nodeType;
    }

    public Object getPercentage(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        if (nodeType != null && !nodeType.equals(context.getNodeType()))
            return null;

        Object currentValue = currentAccessor.get(componentValue, metricValue, context);
        Object baseValue = baseAccessor.get(componentValue, metricValue, context);

        return getPercentage(currentValue, baseValue);
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        if (nodeType != null && !nodeType.equals(context.getNodeType()))
            return null;

        Object currentValue = currentAccessor.get(componentValue, metricValue, context);
        Object baseValue = baseAccessor.get(componentValue, metricValue, context);

        return getPercentage(currentValue, baseValue);
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }

    private Object getPercentage(Object currentValue, Object baseValue) {
        if (currentValue instanceof JsonArray && baseValue instanceof JsonArray) {
            JsonArray current = (JsonArray) currentValue;
            JsonArray base = (JsonArray) baseValue;
            if (current.size() != base.size())
                return null;

            JsonArrayBuilder builder = new JsonArrayBuilder();
            for (int i = 0; i < current.size(); i++) {
                Object currentElement = current.get(i);
                Object baseElement = base.get(i);

                Object value = getPercentage(currentElement, baseElement);
                if (value == null)
                    return null;

                builder.add(value);
            }

            return builder.toJson();
        } else if (currentValue instanceof Number && baseValue instanceof Number) {
            double current = ((Number) currentValue).doubleValue();
            double base = ((Number) baseValue).doubleValue();

            return Numbers.percents(current, base);
        } else
            return null;
    }
}