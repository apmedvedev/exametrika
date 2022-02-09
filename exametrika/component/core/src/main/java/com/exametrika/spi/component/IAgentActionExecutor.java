/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link IAgentActionExecutor} represents a remote executor of component action.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAgentActionExecutor {
    String NAME = "actionExecutor";

    /**
     * Executes specified action asynchronously in given agent.
     *
     * @param <T>               action result type
     * @param agentId           identifier of agent
     * @param action            action to execute in specified agent (action must be serializable and action class must be contained in agent)
     * @param completionHandler completion handler to return results of execution, can be called in arbitrary context
     */
    <T> void execute(String agentId, Object action, ICompletionHandler<T> completionHandler);
}
