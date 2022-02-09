/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.impl.component.actions.EnableMaintenanceModeAction;
import com.exametrika.spi.component.config.model.SyncActionSchemaConfiguration;


/**
 * The {@link EnableMaintenanceModeActionSchemaConfiguration} is a enable maintenance mode action schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class EnableMaintenanceModeActionSchemaConfiguration extends SyncActionSchemaConfiguration {
    public EnableMaintenanceModeActionSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends IAction> T createAction(IComponent component, IActionSchema schema) {
        return (T) new EnableMaintenanceModeAction((IHealthComponent) component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EnableMaintenanceModeActionSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getName() + "(message:string):void";
    }
}
