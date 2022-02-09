/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.nodes.IAggregationNode;


/**
 * The {@link IComponentBindingStrategy} represents a component binding strategy which binds aggregation node to component.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentBindingStrategy {
    /**
     * Returns scope of component bound to specified aggregation node.
     *
     * @param aggregationNode aggregation node
     * @return component scope or null if node is not bound to any component
     */
    IScopeName getComponentScope(IAggregationNode aggregationNode);
}
