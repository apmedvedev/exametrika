/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link ScopeAggregationStrategySchemaConfiguration} represents a configuration of schema of scope aggregation strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ScopeAggregationStrategySchemaConfiguration extends Configuration {
    public abstract IScopeAggregationStrategy createStrategy(IDatabaseContext context);
}
