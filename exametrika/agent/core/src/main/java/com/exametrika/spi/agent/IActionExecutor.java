/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.agent;

import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link IActionExecutor} is an action executor.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IActionExecutor extends ILifecycle {
    /**
     * Does action executor support specified action to execute.
     *
     * @param action action to execute
     * @return true if action executor support specified action
     */
    boolean supports(Object action);

    /**
     * Executes action.
     *
     * @param context action context
     */
    void execute(IActionContext context);
}
