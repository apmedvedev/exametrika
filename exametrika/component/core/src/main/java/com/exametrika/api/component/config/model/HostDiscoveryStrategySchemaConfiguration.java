/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.impl.component.discovery.HostDiscoveryStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link HostDiscoveryStrategySchemaConfiguration} is a host component discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostDiscoveryStrategySchemaConfiguration extends BaseComponentDiscoveryStrategySchemaConfiguration {
    public HostDiscoveryStrategySchemaConfiguration(String component) {
        super(component);
    }

    @Override
    public IComponentDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new HostDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostDiscoveryStrategySchemaConfiguration))
            return false;

        HostDiscoveryStrategySchemaConfiguration configuration = (HostDiscoveryStrategySchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
