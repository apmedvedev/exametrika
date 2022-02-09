/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.SimpleComponentBindingStrategy;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleComponentBindingStrategySchemaConfiguration} is a error log transformer schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleComponentBindingStrategySchemaConfiguration extends ComponentBindingStrategySchemaConfiguration {
    private final boolean hasSubScope;

    public SimpleComponentBindingStrategySchemaConfiguration(boolean hasSubScope) {
        this.hasSubScope = hasSubScope;
    }

    public boolean hasSubScope() {
        return hasSubScope;
    }

    @Override
    public IComponentBindingStrategy createStrategy(IDatabaseContext context) {
        return new SimpleComponentBindingStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleComponentBindingStrategySchemaConfiguration))
            return false;

        SimpleComponentBindingStrategySchemaConfiguration configuration = (SimpleComponentBindingStrategySchemaConfiguration) o;
        return hasSubScope == configuration.hasSubScope;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hasSubScope);
    }
}
