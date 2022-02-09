/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy;

/**
 * The {@link IInterceptor} is a helper object that intercepts a call.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInterceptor {
    /**
     * Called when call is intercepted.
     *
     * @param <T>        return type name
     * @param invocation call invocation
     * @return call return value
     */
    <T> T invoke(IInvocation invocation);
}
