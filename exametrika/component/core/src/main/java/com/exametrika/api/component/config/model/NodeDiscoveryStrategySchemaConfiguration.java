/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.impl.component.discovery.NodeDiscoveryStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeDiscoveryStrategySchemaConfiguration} is a node component discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NodeDiscoveryStrategySchemaConfiguration extends BaseComponentDiscoveryStrategySchemaConfiguration {
    public NodeDiscoveryStrategySchemaConfiguration(String component) {
        super(component);
    }

    @Override
    public IComponentDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new NodeDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeDiscoveryStrategySchemaConfiguration))
            return false;

        NodeDiscoveryStrategySchemaConfiguration configuration = (NodeDiscoveryStrategySchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
