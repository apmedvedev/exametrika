/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import com.exametrika.api.component.config.model.HealthAlertSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.component.IHealthCheck;


/**
 * The {@link HealthAlert} represents a health alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthAlert extends Alert implements IHealthCheck {
    private final HealthAlertSchemaConfiguration configuration;

    public HealthAlert(HealthAlertSchemaConfiguration configuration) {
        super(configuration);

        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return getConfiguration().getName();
    }

    @Override
    public void onStateChanged(IComponent c, State oldState, State newState) {
        ComponentNode component = (ComponentNode) c;
        if ((newState == State.HEALTH_WARNING || newState == State.HEALTH_ERROR || newState == State.UNAVAILABLE) &&
                configuration.getStateThreshold().ordinal() <= newState.ordinal()) {
            IIncident incident = component.findIncident(getConfiguration().getName());
            if (incident != null)
                incident.delete(false);

            component.createIncident(this, false);
        } else if ((oldState == State.HEALTH_WARNING || oldState == State.HEALTH_ERROR || oldState == State.UNAVAILABLE) &&
                configuration.getStateThreshold().ordinal() <= oldState.ordinal()) {
            IIncident incident = component.findIncident(getConfiguration().getName());
            if (incident != null)
                incident.delete(true);
        }
    }
}
