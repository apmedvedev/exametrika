/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.discovery.PatternGroupDiscoveryStrategy;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link PatternGroupDiscoveryStrategySchemaConfiguration} is a pattern group discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PatternGroupDiscoveryStrategySchemaConfiguration extends GroupDiscoveryStrategySchemaConfiguration {
    private final String component;
    private final String pattern;
    private final String group;

    public PatternGroupDiscoveryStrategySchemaConfiguration(String component, String pattern, String group) {
        Assert.notNull(component);
        Assert.notNull(pattern);

        this.component = component;
        this.pattern = pattern;
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public String getComponent() {
        return component;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public IGroupDiscoveryStrategy createStrategy(IDatabaseContext context) {
        return new PatternGroupDiscoveryStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PatternGroupDiscoveryStrategySchemaConfiguration))
            return false;

        PatternGroupDiscoveryStrategySchemaConfiguration configuration = (PatternGroupDiscoveryStrategySchemaConfiguration) o;
        return component.equals(configuration.component) && pattern.equals(configuration.pattern) &&
                Objects.equals(group, configuration.group);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(component, pattern, group);
    }
}
