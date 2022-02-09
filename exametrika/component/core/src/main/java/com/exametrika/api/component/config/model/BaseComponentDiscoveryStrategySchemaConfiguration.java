/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.discovery.BaseComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link BaseComponentDiscoveryStrategySchemaConfiguration} is a base component discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class BaseComponentDiscoveryStrategySchemaConfiguration extends ComponentDiscoveryStrategySchemaConfiguration {
    private final String component;

    public BaseComponentDiscoveryStrategySchemaConfiguration(String component) {
        Assert.notNull(component);

        this.component = component;
    }

    public String getComponent() {
        return component;
    }

    @Override
    public IComponentDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new BaseComponentDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BaseComponentDiscoveryStrategySchemaConfiguration))
            return false;

        BaseComponentDiscoveryStrategySchemaConfiguration configuration = (BaseComponentDiscoveryStrategySchemaConfiguration) o;
        return component.equals(configuration.component);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(component);
    }
}
