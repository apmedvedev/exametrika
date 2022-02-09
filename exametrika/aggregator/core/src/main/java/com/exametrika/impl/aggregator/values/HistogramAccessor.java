/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link HistogramAccessor} is an implementation of {@link IFieldAccessor} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class HistogramAccessor implements IFieldAccessor {
    private final Type type;
    private final HistogramComputer computer;
    private final int percentilePercentage;
    private final boolean percentileValue;

    public enum Type {
        BINS,
        MIN_OOB,
        MAX_OOB,
        PERCENTILES,
        PERCENTILE,
        BINS_PERCENTAGES,
        MIN_OOB_PERCENTAGES,
        MAX_OOB_PERCENTAGES,
        BINS_CUMULATIVE_PERCENTAGES,
        MIN_OOB_CUMULATIVE_PERCENTAGE,
        MAX_OOB_CUMULATIVE_PERCENTAGE,
        SCALE
    }

    public HistogramAccessor(int percentilePercentage, boolean percentileValue, HistogramComputer computer) {
        Assert.notNull(computer);
        Assert.isTrue(percentilePercentage >= 0 && percentilePercentage <= 100);

        this.type = Type.PERCENTILE;
        this.percentilePercentage = percentilePercentage;
        this.percentileValue = percentileValue;
        this.computer = computer;
    }

    public HistogramAccessor(Type type, HistogramComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.percentilePercentage = 0;
        this.percentileValue = false;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IHistogramValue value = (IHistogramValue) v;
        if (value == null)
            return null;

        Long count = (Long) computer.getCountAccessor().get(componentValue, metricValue, context);
        if (count == null)
            return null;
        Long min = (Long) computer.getMinAccessor().get(componentValue, metricValue, context);
        Long max = (Long) computer.getMaxAccessor().get(componentValue, metricValue, context);
        if (min == null || max == null)
            return null;

        switch (type) {
            case BINS:
                return computer.getBins(value);
            case MIN_OOB:
                return value.getMinOutOfBounds();
            case MAX_OOB:
                return value.getMaxOutOfBounds();
            case BINS_PERCENTAGES:
                return computer.getBinsPercentages(value, count);
            case PERCENTILES:
                return computer.getPercentiles(value, count, min, max);
            case PERCENTILE:
                return computer.getPercentile(value, count, min, max, percentilePercentage, percentileValue);
            case MIN_OOB_PERCENTAGES:
                return computer.getMinOobPercentage(value, count);
            case MAX_OOB_PERCENTAGES:
                return computer.getMaxOobPercentage(value, count);
            case BINS_CUMULATIVE_PERCENTAGES:
                return computer.getBinsCumulativePercentages(value, count);
            case MIN_OOB_CUMULATIVE_PERCENTAGE:
                return computer.getMinOobCumulativePercentage(value, count);
            case MAX_OOB_CUMULATIVE_PERCENTAGE:
                return computer.getMaxOobCumulativePercentage(value, count);
            case SCALE:
                return computer.getScale();
            default:
                return Assert.error();
        }
    }
}
