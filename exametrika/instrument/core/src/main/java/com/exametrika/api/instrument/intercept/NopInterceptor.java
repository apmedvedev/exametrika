/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.intercept;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link NopInterceptor} is a interceptor that does nothing.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NopInterceptor implements IDynamicInterceptor {
    @Override
    public boolean intercept(IInvocation invocation) {
        return true;
    }

    @Override
    public void start(IJoinPoint joinPoint) {
    }

    @Override
    public void stop(boolean close) {
    }
}
