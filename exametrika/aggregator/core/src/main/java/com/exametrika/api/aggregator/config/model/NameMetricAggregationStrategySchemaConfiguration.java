/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.NameMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NameMetricAggregationStrategySchemaConfiguration} is a name-based metric aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameMetricAggregationStrategySchemaConfiguration extends MetricAggregationStrategySchemaConfiguration {
    private final String root;

    public NameMetricAggregationStrategySchemaConfiguration(String root) {
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
        if (!(o instanceof NameMetricAggregationStrategySchemaConfiguration))
            return false;

        NameMetricAggregationStrategySchemaConfiguration configuration = (NameMetricAggregationStrategySchemaConfiguration) o;
        return Objects.equals(root, configuration.root);
    }

    @Override
    public IMetricAggregationStrategy createStrategy(IDatabaseContext context) {
        return new NameMetricAggregationStrategy(this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root);
    }
}
