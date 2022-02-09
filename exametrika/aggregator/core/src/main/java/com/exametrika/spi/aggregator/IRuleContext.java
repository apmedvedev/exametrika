/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.Map;


/**
 * The {@link IRuleContext} represents a rule context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRuleContext {
    /**
     * Returns facts map for specified rule executor.
     *
     * @param ruleExecutor rule executor
     * @return facts map used in execution of complex rules
     */
    Map<String, Object> getFacts(IRuleExecutor ruleExecutor);

    /**
     * Returns fact value for specified rule executor and fact name.
     *
     * @param ruleExecutor rule executor
     * @param name         fact name
     * @return fact value
     */
    Object getFact(IRuleExecutor ruleExecutor, String name);

    /**
     * Sets new fact replacing old fact with the same name.
     *
     * @param ruleExecutor rule executor
     * @param name         fact name
     * @param value        fact value
     */
    void setFact(IRuleExecutor ruleExecutor, String name, Object value);

    /**
     * Adds new fact appending it to old facts with the same name.
     *
     * @param ruleExecutor rule executor
     * @param name         fact name
     * @param value        fact value
     */
    void addFact(IRuleExecutor ruleExecutor, String name, Object value);

    /**
     * Increments fact value.
     *
     * @param ruleExecutor rule executor
     * @param name         fact name
     */
    void incrementFact(IRuleExecutor ruleExecutor, String name);
}
