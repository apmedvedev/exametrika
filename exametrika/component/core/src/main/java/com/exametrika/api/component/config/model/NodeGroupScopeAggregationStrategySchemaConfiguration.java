/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.aggregation.NodeGroupScopeAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeGroupScopeAggregationStrategySchemaConfiguration} is a node group-based scope aggregation strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NodeGroupScopeAggregationStrategySchemaConfiguration extends ScopeAggregationStrategySchemaConfiguration {
    private final String hierarchyType;

    public NodeGroupScopeAggregationStrategySchemaConfiguration(String hierarchyType) {
        Assert.notNull(hierarchyType);

        this.hierarchyType = hierarchyType;
    }

    public String getHierarchyType() {
        return hierarchyType;
    }

    @Override
    public IScopeAggregationStrategy createStrategy(IDatabaseContext context) {
        return new NodeGroupScopeAggregationStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeGroupScopeAggregationStrategySchemaConfiguration))
            return false;

        NodeGroupScopeAggregationStrategySchemaConfiguration configuration = (NodeGroupScopeAggregationStrategySchemaConfiguration) o;
        return hierarchyType.equals(configuration.hierarchyType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hierarchyType);
    }
}
