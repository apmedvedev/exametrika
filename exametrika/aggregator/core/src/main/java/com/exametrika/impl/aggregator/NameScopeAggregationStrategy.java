/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.ScopeHierarchy;


/**
 * The {@link NameScopeAggregationStrategy} is a name-based scope aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameScopeAggregationStrategy implements IScopeAggregationStrategy {
    @Override
    public ScopeHierarchy getAggregationHierarchy(IAggregationNode node) {
        IScopeName scope = node.getScope();
        List<IScopeName> scopes = new ArrayList<IScopeName>();

        StringBuilder nameBuilder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < scope.getSegments().size(); i++) {
            if (first)
                first = false;
            else
                nameBuilder.append('.');

            nameBuilder.append(scope.getSegments().get(i));
            scopes.add(ScopeName.get(nameBuilder.toString()));
        }

        return new ScopeHierarchy(scopes);
    }

    @Override
    public boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node) {
        return false;
    }
}
