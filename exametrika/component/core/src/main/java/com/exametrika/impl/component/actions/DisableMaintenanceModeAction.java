/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.Map;

import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.schema.IActionSchema;


/**
 * The {@link DisableMaintenanceModeAction} is a enable maintenance mode action.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class DisableMaintenanceModeAction extends Action {
    public DisableMaintenanceModeAction(IHealthComponent component, IActionSchema schema) {
        super(component, schema);
    }

    @Override
    protected void doExecute(Map<String, ?> parameters) {
        ((IHealthComponent) getComponent()).disableMaintenanceMode();
    }
}
