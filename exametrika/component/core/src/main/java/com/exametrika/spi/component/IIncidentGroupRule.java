/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;


/**
 * The {@link IIncidentGroupRule} represents a incident group component rule.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIncidentGroupRule extends IRule {
    /**
     * Called when new active incident has been created to child component.
     *
     * @param component component where alert group is defined
     * @param incident  child incident
     */
    void onIncidentCreated(IComponent component, IIncident incident);
}
