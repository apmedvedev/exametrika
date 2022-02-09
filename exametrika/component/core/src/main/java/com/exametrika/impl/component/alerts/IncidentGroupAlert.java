/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.IncidentGroupNode;
import com.exametrika.spi.component.IIncidentGroupRule;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;


/**
 * The {@link IncidentGroupAlert} represents a base incident group alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class IncidentGroupAlert extends Alert implements IIncidentGroupRule {
    public IncidentGroupAlert(AlertSchemaConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return getConfiguration().getName();
    }

    @Override
    public void onIncidentCreated(IComponent c, IIncident incident) {
        if (!isMatched(incident))
            return;

        ComponentNode component = (ComponentNode) c;
        IncidentGroupNode incidentGroup = (IncidentGroupNode) component.findIncident(getConfiguration().getName());

        if (incidentGroup == null)
            incidentGroup = (IncidentGroupNode) component.createIncident(this, true);

        incidentGroup.addChild(incident);
    }

    protected abstract boolean isMatched(IIncident incident);
}
