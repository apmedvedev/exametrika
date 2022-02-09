/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;


/**
 * The {@link IAction} represents a component action.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAction {
    /**
     * Returns action schema.
     *
     * @return action schema
     */
    IActionSchema getSchema();

    /**
     * Returns component bound to this action.
     *
     * @return component bound to this action
     */
    IComponent getComponent();

    /**
     * Executes action.
     *
     * @param parameters action parameters
     */
    void execute(Map<String, ?> parameters);
}
