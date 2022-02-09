/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.discovery.TransactionGroupDiscoveryStrategy;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TransactionGroupDiscoveryStrategySchemaConfiguration} is a transaction group discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TransactionGroupDiscoveryStrategySchemaConfiguration extends GroupDiscoveryStrategySchemaConfiguration {
    private final String component;
    private final String group;

    public TransactionGroupDiscoveryStrategySchemaConfiguration(String component, String group) {
        Assert.notNull(component);

        this.component = component;
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public String getComponent() {
        return component;
    }

    @Override
    public IGroupDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new TransactionGroupDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TransactionGroupDiscoveryStrategySchemaConfiguration))
            return false;

        TransactionGroupDiscoveryStrategySchemaConfiguration configuration = (TransactionGroupDiscoveryStrategySchemaConfiguration) o;
        return component.equals(configuration.component) && Objects.equals(group, configuration.group);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(component, group);
    }
}
