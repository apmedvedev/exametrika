/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.SimpleMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleMetricAggregationStrategySchemaConfiguration} is a simple metric aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleMetricAggregationStrategySchemaConfiguration extends MetricAggregationStrategySchemaConfiguration {
    private final String root;

    public SimpleMetricAggregationStrategySchemaConfiguration(String root) {
        if (root != null)
            Assert.isTrue(!root.isEmpty());

        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleMetricAggregationStrategySchemaConfiguration))
            return false;

        SimpleMetricAggregationStrategySchemaConfiguration configuration = (SimpleMetricAggregationStrategySchemaConfiguration) o;
        return Objects.equals(root, configuration.root);
    }

    @Override
    public IMetricAggregationStrategy createStrategy(IDatabaseContext context) {
        return new SimpleMetricAggregationStrategy(this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root);
    }
}
