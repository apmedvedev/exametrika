/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link CompletionHandler} is completion handler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CompletionHandler<T> implements ICompletionHandler<T> {
    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void onSucceeded(T result) {
        onCompleted(result);
    }

    @Override
    public void onFailed(Throwable error) {
        onCompleted(error);
    }

    protected void onCompleted(Object result) {
    }
}
