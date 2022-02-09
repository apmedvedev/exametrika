/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.Map;

import com.exametrika.api.aggregator.nodes.IAggregationNode;


/**
 * The {@link IRuleExecutor} represents a  executor of rules.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IRuleExecutor {
    /**
     * Returns scope identifier of rule executor component.
     *
     * @return scope identifier of rule executor component
     */
    long getScopeId();

    /**
     * Executes simple rules in context of specified aggregation node.
     *
     * @param aggregationNode aggregation node
     * @param context         rule context
     */
    void executeSimpleRules(IAggregationNode aggregationNode, IRuleContext context);

    /**
     * Executes complex rules in context of specified facts.
     *
     * @param facts facts map
     */
    void executeComplexRules(Map<String, Object> facts);
}
