/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.schema.SelectorSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link SelectorSchemaConfiguration} represents a configuration of schema of component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SelectorSchemaConfiguration extends Configuration {
    private final String name;

    public SelectorSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ISelectorSchema createSchema(ISchemaObject component, IDatabaseContext context) {
        return new SelectorSchema(name, this, context, component);
    }

    public abstract <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SelectorSchemaConfiguration))
            return false;

        SelectorSchemaConfiguration configuration = (SelectorSchemaConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
