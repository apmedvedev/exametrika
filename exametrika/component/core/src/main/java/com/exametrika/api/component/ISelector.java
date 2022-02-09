/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;


/**
 * The {@link ISelector} represents a component selector used to select aggregated measurements.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISelector {
    /**
     * Returns selector schema.
     *
     * @return action schema
     */
    ISelectorSchema getSchema();

    /**
     * Returns component bound to this selector.
     *
     * @return component bound to this selector
     */
    IComponent getComponent();

    /**
     * Performs selection.
     *
     * @param parameters selector parameters
     * @return result of selection in json form
     */
    Object select(Map<String, ?> parameters);
}
