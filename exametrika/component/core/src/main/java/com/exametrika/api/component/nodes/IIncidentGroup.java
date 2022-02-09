/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IIncidentGroup} represents an incident group.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIncidentGroup extends IIncident {
    /**
     * Returns children incidents.
     *
     * @return children incidents
     */
    Iterable<IIncident> getChildren();
}
