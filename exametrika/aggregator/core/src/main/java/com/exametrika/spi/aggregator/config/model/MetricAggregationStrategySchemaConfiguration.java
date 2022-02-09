/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link MetricAggregationStrategySchemaConfiguration} represents a configuration of schema of metric aggregation strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MetricAggregationStrategySchemaConfiguration extends Configuration {
    public abstract IMetricAggregationStrategy createStrategy(IDatabaseContext context);
}
