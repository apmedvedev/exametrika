/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IHealthComponent} represents a health component node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHealthComponent extends IComponent {
    /**
     * Enables maintenance mode of component.
     *
     * @param message maintenance message
     */
    void enableMaintenanceMode(String message);

    /**
     * Disables maintenance mode of component.
     */
    void disableMaintenanceMode();
}
