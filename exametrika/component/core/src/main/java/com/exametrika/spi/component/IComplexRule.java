/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;


/**
 * The {@link IComplexRule} represents a complex component rule, whose execution depends on executions of several simple rules.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComplexRule extends IRule {
    /**
     * Executes rule in context of specified component and facts map.
     *
     * @param component component
     * @param facts     map of facts gathered by simple rules
     */
    void execute(IComponent component, Map<String, Object> facts);
}
