/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link ICompletionHandler} represents a asynchronous operation completion handler.
 *
 * @param <T> result type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompletionHandler<T> {
    /**
     * Is requestor canceled and activity, which requires completion, can be canceled?
     *
     * @return true if requestor is canceled and completion notification is not required any more
     */
    boolean isCanceled();

    /**
     * Marks operation execution as completed successfully and sets result of operation execution.
     *
     * @param result result of operation execution
     */
    void onSucceeded(T result);

    /**
     * Marks operation execution as failed and sets error of operation execution.
     *
     * @param error error of operation execution
     */
    void onFailed(Throwable error);
}
