/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.List;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;


/**
 * The {@link IGroupDiscoveryStrategy} represents a group discovery strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IGroupDiscoveryStrategy {
    /**
     * Returns list of component groups.
     *
     * @param initialComponent initial component triggered group discovery
     * @param childComponent   child component
     * @param level            hierarchy level
     * @return list of component groups, if child component is group only one parent group must be returned
     */
    List<IGroupComponent> getGroups(IComponent initialComponent, IComponent childComponent, int level);
}
