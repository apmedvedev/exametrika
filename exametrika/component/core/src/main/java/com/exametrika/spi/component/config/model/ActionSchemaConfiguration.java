/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.schema.ActionSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link ActionSchemaConfiguration} represents a configuration of schema of component action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ActionSchemaConfiguration extends Configuration {
    private final String name;

    public ActionSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IActionSchema createSchema(ISchemaObject component, IDatabaseContext context) {
        return new ActionSchema(name, this, context, component);
    }

    public abstract boolean isAsync();

    public abstract <T extends IAction> T createAction(IComponent component, IActionSchema schema);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ActionSchemaConfiguration))
            return false;

        ActionSchemaConfiguration configuration = (ActionSchemaConfiguration) o;
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
