/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link PeriodAccessor} is an implementation of {@link IFieldAccessor} for period fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodAccessor implements IFieldAccessor {
    private final Type type;
    private final PeriodComputer computer;

    public enum Type {
        DELTA,
        DELTA_PERCENTAGE,
    }

    public PeriodAccessor(Type type, PeriodComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        switch (type) {
            case DELTA:
                return computer.getDelta(componentValue, metricValue, context, false);
            case DELTA_PERCENTAGE:
                return computer.getDelta(componentValue, metricValue, context, true);
            default:
                return Assert.error();
        }
    }
}
