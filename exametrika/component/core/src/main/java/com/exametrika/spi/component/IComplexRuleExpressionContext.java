/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IComplexRuleExpressionContext} represents a complex rule expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComplexRuleExpressionContext extends IExpressionContext {
    /**
     * Returns base component.
     *
     * @return base component
     */
    IComponent getComponent();

    /**
     * Returns list of facts.
     *
     * @return list of facts
     */
    Map<String, Object> getFacts();

    /**
     * Do facts have specified fact?
     *
     * @param name fact name
     * @return true if facts have specified fact
     */
    boolean hasFact(String name);

    /**
     * Returns fact by name.
     *
     * @param name fact name
     * @return fact value or null if fact is not found
     */
    Object fact(String name);

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
