/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.discovery.SimpleGroupDiscoveryStrategy;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleGroupDiscoveryStrategySchemaConfiguration} is a simple group discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleGroupDiscoveryStrategySchemaConfiguration extends GroupDiscoveryStrategySchemaConfiguration {
    private final String group;

    public SimpleGroupDiscoveryStrategySchemaConfiguration(String group) {
        Assert.notNull(group);

        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public IGroupDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new SimpleGroupDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleGroupDiscoveryStrategySchemaConfiguration))
            return false;

        SimpleGroupDiscoveryStrategySchemaConfiguration configuration = (SimpleGroupDiscoveryStrategySchemaConfiguration) o;
        return group.equals(configuration.group);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(group);
    }
}
