/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IGroupComponentVersion} represents a group component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IGroupComponentVersion extends IHealthComponentVersion {
    /**
     * Is group predefined?
     *
     * @return true if group is predefined
     */
    boolean isPredefined();

    /**
     * Returns parent.
     *
     * @return parent
     */
    IGroupComponent getParent();

    /**
     * Returns children.
     *
     * @return children
     */
    Iterable<IGroupComponent> getChildren();

    /**
     * Returns components.
     *
     * @return components
     */
    Iterable<IComponent> getComponents();
}
