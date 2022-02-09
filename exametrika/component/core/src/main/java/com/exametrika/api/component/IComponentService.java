/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import com.exametrika.api.component.nodes.IBehaviorType;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IIncident;

/**
 * The {@link IComponentService} represents a component service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComponentService {
    String NAME = "component.ComponentService";

    /**
     * Returns root group.
     *
     * @return root group
     */
    IGroupComponent getRootGroup();

    /**
     * Returns list of incidents.
     *
     * @return list of incidents
     */
    Iterable<IIncident> getIncidents();

    /**
     * Returns component by scope name.
     *
     * @param scopeName scope name
     * @return component or null if component is not found
     */
    <T extends IComponent> T findComponent(String scopeName);

    /**
     * Returns component by scope identifier.
     *
     * @param scopeId scope id
     * @return component or null if component is not found
     */
    <T extends IComponent> T findComponent(long scopeId);

    /**
     * Creates group
     *
     * @param scopeName scope name
     * @param groupType group type
     * @return group
     */
    IGroupComponent createGroup(String scopeName, String groupType);

    /**
     * Finds behavior type by type identifier.
     *
     * @param typeId type identifier
     * @return behavior type or null if behavior type is not found
     */
    IBehaviorType findBehaviorType(long typeId);
}
