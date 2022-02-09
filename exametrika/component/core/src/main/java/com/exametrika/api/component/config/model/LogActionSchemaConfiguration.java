/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.impl.component.actions.LogAction;
import com.exametrika.spi.component.config.model.SyncActionSchemaConfiguration;


/**
 * The {@link LogActionSchemaConfiguration} is a log action schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class LogActionSchemaConfiguration extends SyncActionSchemaConfiguration {
    public LogActionSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends IAction> T createAction(IComponent component, IActionSchema schema) {
        return (T) new LogAction(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogActionSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getName() + "(action:string):void";
    }
}
