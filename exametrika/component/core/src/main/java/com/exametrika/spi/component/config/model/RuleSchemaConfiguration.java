/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.IRule;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link RuleSchemaConfiguration} represents a configuration of schema of component rule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class RuleSchemaConfiguration extends Configuration {
    private final String name;
    private final boolean enabled;

    public RuleSchemaConfiguration(String name, boolean enabled) {
        Assert.notNull(name);

        this.name = name;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract IRule createRule(IDatabaseContext context);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RuleSchemaConfiguration))
            return false;

        RuleSchemaConfiguration configuration = (RuleSchemaConfiguration) o;
        return name.equals(configuration.name) && enabled == configuration.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, enabled);
    }

    @Override
    public String toString() {
        return name;
    }
}
