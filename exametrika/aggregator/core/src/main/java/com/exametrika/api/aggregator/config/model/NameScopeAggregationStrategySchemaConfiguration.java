/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.impl.aggregator.NameScopeAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NameScopeAggregationStrategySchemaConfiguration} is a name-based scope aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameScopeAggregationStrategySchemaConfiguration extends ScopeAggregationStrategySchemaConfiguration {
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameScopeAggregationStrategySchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public IScopeAggregationStrategy createStrategy(IDatabaseContext context) {
        return new NameScopeAggregationStrategy();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
