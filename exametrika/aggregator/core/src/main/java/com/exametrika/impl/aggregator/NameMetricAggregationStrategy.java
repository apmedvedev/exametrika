/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.config.model.NameMetricAggregationStrategySchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.MetricHierarchy;


/**
 * The {@link NameMetricAggregationStrategy} is a name-based metric aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameMetricAggregationStrategy implements IMetricAggregationStrategy {
    private final NameMetricAggregationStrategySchemaConfiguration configuration;

    public NameMetricAggregationStrategy(NameMetricAggregationStrategySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public MetricHierarchy getAggregationHierarchy(IMetricName metric) {
        List<IMetricName> metrics = new ArrayList<IMetricName>();

        StringBuilder nameBuilder = new StringBuilder();
        boolean first = true;
        if (configuration.getRoot() != null) {
            metrics.add(Names.getMetric(configuration.getRoot()));
            first = false;
        }

        if (!metric.isEmpty()) {
            first = true;
            for (int i = 0; i < metric.getSegments().size(); i++) {
                if (first)
                    first = false;
                else
                    nameBuilder.append('.');

                nameBuilder.append(metric.getSegments().get(i));
                metrics.add(MetricName.get(nameBuilder.toString()));
            }
        } else if (first)
            metrics.add(Names.rootMetric());

        return new MetricHierarchy(metrics);
    }
}
