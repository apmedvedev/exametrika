/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IRuleService} represents a rule service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRuleService {
    String NAME = "component.RuleService";

    /**
     * Finds rule executor by scope identifier.
     *
     * @param scopeId scope identifier
     * @return rule executor or null if rule executor is not found
     */
    IRuleExecutor findRuleExecutor(long scopeId);
}
