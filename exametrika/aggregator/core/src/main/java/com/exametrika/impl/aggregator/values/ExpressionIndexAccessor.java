/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link ExpressionIndexAccessor} is an implementation of {@link IFieldAccessor} for expression index fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExpressionIndexAccessor implements IMetricAccessor {
    private final boolean stored;
    private final ExpressionIndexComputer computer;

    public ExpressionIndexAccessor(boolean stored, ExpressionIndexComputer computer) {
        this.stored = stored;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        if (stored) {
            if (metricValue != null)
                return ((IObjectValue) metricValue).getObject();
            else
                return null;
        } else
            return computer.compute(componentValue, metricValue, context);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
