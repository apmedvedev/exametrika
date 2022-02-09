/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IHealthCheckExpressionContext} represents a health check expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IHealthCheckExpressionContext extends IExpressionContext {
    /**
     * Returns base component.
     *
     * @return base component
     */
    IComponent getComponent();

    /**
     * Returns old (previous) state.
     *
     * @return old (previous) state
     */
    String getOldState();

    /**
     * Returns new (next) state.
     *
     * @return new (next) state
     */
    String getNewState();

    /**
     * Executes action.
     *
     * @param name       action name
     * @param parameters action parameters
     */
    void action(String name, Map<String, ?> parameters);

    /**
     * Executes action of specified component.
     *
     * @param component  component
     * @param name       action name
     * @param parameters action parameters
     */
    void action(IComponent component, String name, Map<String, ?> parameters);
}
