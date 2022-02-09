/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.config.model.SimpleComponentBindingStrategySchemaConfiguration;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;


/**
 * The {@link SimpleComponentBindingStrategy} is an implementation of {@link IComponentBindingStrategy}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SimpleComponentBindingStrategy implements IComponentBindingStrategy {
    private SimpleComponentBindingStrategySchemaConfiguration configuration;

    public SimpleComponentBindingStrategy(SimpleComponentBindingStrategySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public IScopeName getComponentScope(IAggregationNode aggregationNode) {
        if (aggregationNode instanceof INameNode) {
            if (configuration.hasSubScope()) {
                IScopeName scope = aggregationNode.getScope();
                return Names.getScope(scope.getSegments().subList(0, scope.getSegments().size() - 1));
            }
        } else if (aggregationNode instanceof IStackNode) {
            IEntryPointNode root = ((IStackNode) aggregationNode).getTransactionRoot();
            if (root != null)
                return root.getScope();
        } else if (aggregationNode instanceof IStackLogNode) {
            IStackNode mainNode = ((IStackLogNode) aggregationNode).getMainNode();
            if (mainNode != null)
                return getComponentScope(mainNode);
        }

        return aggregationNode.getScope();
    }
}
