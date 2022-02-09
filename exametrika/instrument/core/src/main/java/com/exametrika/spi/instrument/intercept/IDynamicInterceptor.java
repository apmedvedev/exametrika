/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.intercept;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;


/**
 * The {@link IDynamicInterceptor} represents an interceptor. Interceptor must have constructor without any parameters.
 * Instances of interceptors are created per join point.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDynamicInterceptor {
    /**
     * Called on intercepted event. Interceptor must be prepared to accept interception calls not only in started but also in stopped state.
     *
     * @param invocation current invocation context
     * @return if false dynamically disables subsequent interceptions
     */
    boolean intercept(IInvocation invocation);

    /**
     * Starts the interceptor. When interceptor is started it can be stopped.
     *
     * @param joinPoint interceptor's join point
     */
    void start(IJoinPoint joinPoint);

    /**
     * Stops the interceptor. When interceptor is stopped it can be restarted.
     *
     * @param close if true instrumentation agent is closing
     */
    void stop(boolean close);
}
