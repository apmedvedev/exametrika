/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.intercept;

import com.exametrika.common.utils.Times;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.intercept.AbstractDynamicInterceptor;


/**
 * The {@link Meter} is used to meter interval. Meter is not reentrant.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Meter extends AbstractDynamicInterceptor {
    private final ThreadLocal<Long> start = new ThreadLocal<Long>();

    @Override
    protected void updateValue(IInvocation invocation) {
        if (invocation.getKind() == IInvocation.Kind.INTERCEPT)
            return;

        if (invocation.getKind() == IInvocation.Kind.ENTER)
            start.set(Times.getCurrentTime());
        else {
            Long startValue = start.get();
            if (startValue != null)
                value.addAndGet(Times.getCurrentTime() - startValue);
        }
    }
}
