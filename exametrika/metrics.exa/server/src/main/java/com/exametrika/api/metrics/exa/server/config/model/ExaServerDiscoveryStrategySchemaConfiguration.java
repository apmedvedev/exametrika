/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config.model;

import com.exametrika.api.component.config.model.BaseComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.impl.metrics.exa.server.discovery.ExaServerDiscoveryStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExaServerDiscoveryStrategySchemaConfiguration} is a exa server component discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerDiscoveryStrategySchemaConfiguration extends BaseComponentDiscoveryStrategySchemaConfiguration {
    public ExaServerDiscoveryStrategySchemaConfiguration(String component) {
        super(component);
    }

    @Override
    public IComponentDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new ExaServerDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerDiscoveryStrategySchemaConfiguration))
            return false;

        ExaServerDiscoveryStrategySchemaConfiguration configuration = (ExaServerDiscoveryStrategySchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
