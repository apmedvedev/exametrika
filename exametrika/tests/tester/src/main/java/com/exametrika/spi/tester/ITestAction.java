/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.util.Map;

import com.exametrika.common.tasks.ITaskContext;


/**
 * The {@link ITestAction} is a test action.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestAction {
    /**
     * Can action be activated.
     *
     * @param currentTime current time
     * @param context     task context
     * @return true if action can be activated
     */
    boolean canActivate(long currentTime, ITaskContext context);

    /**
     * Called when action has been completed.
     *
     * @param context task context
     */
    void onCompleted(ITaskContext context);

    /**
     * Returns action parameters.
     *
     * @return action parameters
     */
    Map<String, Object> getParameters();
}
