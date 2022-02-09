/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link ExceptionProbeInterceptor} represents a static interceptor of exception probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExceptionProbeInterceptor extends StaticInterceptor {
    public static IInterceptor interceptor;

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        IInterceptor interceptor = ExceptionProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            ExceptionProbeInterceptor.interceptor = null;
        }
    }
}
