/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.nodes.IComponent;


/**
 * The {@link IHealthCheck} represents a health check rule.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHealthCheck extends IRule {
    /**
     * Called when state of component has been changed.
     *
     * @param component component
     * @param oldState  old state
     * @param newState  new state
     */
    void onStateChanged(IComponent component, State oldState, State newState);
}
