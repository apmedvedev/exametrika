/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.impl.component.discovery.NodeDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.config.model.ComponentDeletionStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeDeletionStrategySchemaConfiguration} is a node component deletion strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NodeDeletionStrategySchemaConfiguration extends ComponentDeletionStrategySchemaConfiguration {
    @Override
    public IComponentDeletionStrategy createStrategy(IDatabaseContext context) {
        return new NodeDeletionStrategy(context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeDeletionStrategySchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
