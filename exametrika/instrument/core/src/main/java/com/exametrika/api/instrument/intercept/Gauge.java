/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.intercept;

import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.intercept.AbstractDynamicInterceptor;


/**
 * The {@link Gauge} is used to meter current value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Gauge extends AbstractDynamicInterceptor {
    @Override
    protected void updateValue(IInvocation invocation) {
        if (invocation.getKind() == IInvocation.Kind.INTERCEPT || invocation.getKind() == IInvocation.Kind.ENTER)
            value.incrementAndGet();
        else
            value.decrementAndGet();
    }
}
