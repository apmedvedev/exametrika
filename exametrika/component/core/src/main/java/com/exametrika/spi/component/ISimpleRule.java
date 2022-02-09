/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.spi.aggregator.IRuleContext;


/**
 * The {@link ISimpleRule} represents a simple component rule, whose execution depends only on specified component and aggregation node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISimpleRule extends IRule {
    /**
     * Executes rule in context of specified component and aggregation node.
     *
     * @param component component
     * @param node      aggregation node
     * @param context   rule context or null if execution called from non-aggregating period
     */
    void execute(IComponent component, IAggregationNode node, IRuleContext context);
}
