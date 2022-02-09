/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import java.util.Map;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link IAsyncAction} represents a component asynchronous action.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAsyncAction extends IAction {
    /**
     * Executes action.
     *
     * @param <T>               action result type
     * @param parameters        action parameters
     * @param completionHandler completion handler to return results of execution, which is called in context of transaction, it can be null if not used.
     */
    <T> void execute(Map<String, ?> parameters, ICompletionHandler<T> completionHandler);
}
