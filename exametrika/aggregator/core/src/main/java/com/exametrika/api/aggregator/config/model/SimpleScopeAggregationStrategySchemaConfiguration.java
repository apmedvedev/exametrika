/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.SimpleScopeAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleScopeAggregationStrategySchemaConfiguration} is a group-based scope aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleScopeAggregationStrategySchemaConfiguration extends ScopeAggregationStrategySchemaConfiguration {
    private final boolean hasSubScope;

    public SimpleScopeAggregationStrategySchemaConfiguration(boolean hasSubScope) {
        this.hasSubScope = hasSubScope;
    }

    public boolean hasSubScope() {
        return hasSubScope;
    }

    @Override
    public IScopeAggregationStrategy createStrategy(IDatabaseContext context) {
        return new SimpleScopeAggregationStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleScopeAggregationStrategySchemaConfiguration))
            return false;

        SimpleScopeAggregationStrategySchemaConfiguration configuration = (SimpleScopeAggregationStrategySchemaConfiguration) o;
        return hasSubScope == configuration.hasSubScope;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hasSubScope);
    }
}
