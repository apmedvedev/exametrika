/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;


/**
 * The {@link IScopeAggregationStrategy} represents an aggregation strategy for scopes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IScopeAggregationStrategy {
    /**
     * Returns aggregation hierarchy for scope name of specified node.
     *
     * @param aggregationNode aggregation node
     * @return aggregation hierarchy
     */
    ScopeHierarchy getAggregationHierarchy(IAggregationNode aggregationNode);

    /**
     * Is aggregation of secondary entry point allowed?
     *
     * @param transactionAggregation if true transaction is aggregated else background root is aggregated
     * @param node                   secondary entry point
     * @return true if aggregation of specified secondary entry point is allowed
     */
    boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node);
}
