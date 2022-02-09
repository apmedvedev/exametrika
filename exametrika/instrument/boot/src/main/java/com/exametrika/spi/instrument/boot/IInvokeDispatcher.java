/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link IInvokeDispatcher} represents a dispatcher of invocation requests.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInvokeDispatcher {
    /**
     * Invokes interceptor.
     *
     * @param interceptorIndex interceptor index
     * @param version          interceptor version
     * @param invocation       invocation context
     * @return if false dynamically disables subsequent interceptions
     */
    boolean invoke(int interceptorIndex, int version, IInvocation invocation);
}
