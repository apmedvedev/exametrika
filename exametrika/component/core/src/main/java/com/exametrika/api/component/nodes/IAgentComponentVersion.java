/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IAgentComponentVersion} represents an agent component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAgentComponentVersion extends IHealthComponentVersion {
    /**
     * Returns agent sub-components.
     *
     * @return agent sub-components
     */
    Iterable<IComponent> getSubComponents();
}
