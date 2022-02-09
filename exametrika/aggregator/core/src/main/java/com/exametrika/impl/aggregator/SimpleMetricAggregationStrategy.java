/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.Arrays;
import java.util.Collections;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.config.model.SimpleMetricAggregationStrategySchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.MetricHierarchy;


/**
 * The {@link SimpleMetricAggregationStrategy} is a simple metric aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleMetricAggregationStrategy implements IMetricAggregationStrategy {
    private final SimpleMetricAggregationStrategySchemaConfiguration configuration;

    public SimpleMetricAggregationStrategy(SimpleMetricAggregationStrategySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public MetricHierarchy getAggregationHierarchy(IMetricName metric) {
        if (configuration.getRoot() == null)
            return new MetricHierarchy(Collections.singletonList(metric));
        else if (metric.isEmpty())
            return new MetricHierarchy(Collections.singletonList(Names.getMetric(configuration.getRoot())));
        else
            return new MetricHierarchy(Arrays.asList(Names.getMetric(configuration.getRoot()), metric));
    }
}
