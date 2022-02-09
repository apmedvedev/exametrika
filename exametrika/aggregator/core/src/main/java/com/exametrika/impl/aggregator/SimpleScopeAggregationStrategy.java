/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.Arrays;
import java.util.Collections;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.config.model.SimpleScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.ScopeHierarchy;


/**
 * The {@link SimpleScopeAggregationStrategy} is a simple scope aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleScopeAggregationStrategy implements IScopeAggregationStrategy {
    private final SimpleScopeAggregationStrategySchemaConfiguration configuration;

    public SimpleScopeAggregationStrategy(SimpleScopeAggregationStrategySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public ScopeHierarchy getAggregationHierarchy(IAggregationNode node) {
        IScopeName scope = node.getScope();
        if (configuration.hasSubScope())
            return new ScopeHierarchy(Arrays.asList(Names.getScope(scope.getSegments().subList(0, scope.getSegments().size() - 1)), scope));
        else
            return new ScopeHierarchy(Collections.singletonList(scope));
    }

    @Override
    public boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node) {
        return false;
    }
}
