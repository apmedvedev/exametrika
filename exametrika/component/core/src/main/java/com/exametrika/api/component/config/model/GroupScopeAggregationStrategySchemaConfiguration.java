/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.aggregation.GroupScopeAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupScopeAggregationStrategySchemaConfiguration} is a group-based scope aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupScopeAggregationStrategySchemaConfiguration extends ScopeAggregationStrategySchemaConfiguration {
    private final String hierarchyType;
    private final boolean hasSubScope;

    public GroupScopeAggregationStrategySchemaConfiguration(String hierarchyType, boolean hasSubScope) {
        Assert.notNull(hierarchyType);

        this.hierarchyType = hierarchyType;
        this.hasSubScope = hasSubScope;
    }

    public String getHierarchyType() {
        return hierarchyType;
    }

    public boolean hasSubScope() {
        return hasSubScope;
    }

    @Override
    public IScopeAggregationStrategy createStrategy(IDatabaseContext context) {
        return new GroupScopeAggregationStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupScopeAggregationStrategySchemaConfiguration))
            return false;

        GroupScopeAggregationStrategySchemaConfiguration configuration = (GroupScopeAggregationStrategySchemaConfiguration) o;
        return hierarchyType.equals(configuration.hierarchyType) && hasSubScope == configuration.hasSubScope;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hierarchyType, hasSubScope);
    }
}
