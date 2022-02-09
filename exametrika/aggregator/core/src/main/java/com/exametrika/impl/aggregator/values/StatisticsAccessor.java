/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.StatisticsComputer.Statistics;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link StatisticsAccessor} is an implementation of {@link IFieldAccessor} for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StatisticsAccessor implements IFieldAccessor {
    private final Type type;
    private final StatisticsComputer computer;

    public enum Type {
        STANDARD_DEVIATION,
        VARIATION_COEFFICIENT
    }

    public StatisticsAccessor(Type type, StatisticsComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        Statistics statistics = computer.getStatistics(componentValue, metricValue, value, context);
        if (statistics == null)
            return null;

        switch (type) {
            case STANDARD_DEVIATION:
                return statistics.standardDeviation;
            case VARIATION_COEFFICIENT:
                return statistics.variationCoefficient;
            default:
                return Assert.error();
        }
    }
}
