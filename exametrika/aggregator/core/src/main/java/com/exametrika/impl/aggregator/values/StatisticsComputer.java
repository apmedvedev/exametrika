/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStatisticsValue;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link StatisticsComputer} is an implementation of {@link IFieldComputer} for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsComputer implements IFieldComputer {
    private final IMetricAccessor countAccessor;
    private final IMetricAccessor averageAccessor;

    public static class Statistics {
        public final double standardDeviation;
        public final double variationCoefficient;

        public Statistics(double standardDeviation, double variationCoefficient) {
            this.standardDeviation = standardDeviation;
            this.variationCoefficient = variationCoefficient;
        }
    }

    public StatisticsComputer(IMetricAccessor countAccessor, IMetricAccessor averageAccessor) {
        Assert.notNull(countAccessor);
        Assert.notNull(averageAccessor);

        this.countAccessor = countAccessor;
        this.averageAccessor = averageAccessor;
    }

    public Statistics getStatistics(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IStatisticsValue value = (IStatisticsValue) v;
        Long count = (Long) countAccessor.get(componentValue, metricValue, context);
        Double average = (Double) averageAccessor.get(componentValue, metricValue, context);

        if (count != null && average != null) {
            double averageSquare = value.getSumSquares() / count;

            double standardDeviation = Math.sqrt(averageSquare - average * average);
            return new Statistics(standardDeviation, standardDeviation / average);
        } else
            return null;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        Statistics statistics = getStatistics(componentValue, metricValue, value, context);
        if (statistics == null)
            return null;

        JsonObjectBuilder fields = new JsonObjectBuilder();

        fields.put("stddev", statistics.standardDeviation);
        fields.put("vc", statistics.variationCoefficient);

        return fields.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }
}
