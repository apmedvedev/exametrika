/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Map;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.spi.aggregator.IMeasurementExpressionContext;


/**
 * The {@link ISimpleRuleExpressionContext} represents a simple rule expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISimpleRuleExpressionContext extends IMeasurementExpressionContext {
    /**
     * Returns base component.
     *
     * @return base component
     */
    IComponent getComponent();

    /**
     * Returns measurement.
     *
     * @return measurement
     */
    IAggregationNode getMeasurement();

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
     * Adds or replaces fact by name.
     *
     * @param name  fact name
     * @param value fact value
     */
    void fact(String name, Object value);

    /**
     * Adds fact with non-unique name.
     *
     * @param name  fact name
     * @param value fact value
     */
    void addFact(String name, Object value);

    /**
     * Increments fact numeric value.
     *
     * @param name fact name
     */
    void incFact(String name);

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
