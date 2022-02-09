/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.StaticInterceptor;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;


/**
 * The {@link StackProbeInterceptor} represents a stack probe interceptor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackProbeInterceptor extends StaticInterceptor {
    public static IStackInterceptor estimationInterceptor;
    public static IStackInterceptor interceptor;
    public static Object[] methods;

    public static void onReturn(Object param) {
        if (param instanceof ThreadLocalContainer) {
            ThreadLocalContainer container = (ThreadLocalContainer) param;
            container.inCall = false;
            return;
        }

        IStackInterceptor interceptor = StackProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturn(param);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            StackProbeInterceptor.interceptor = null;
        }
    }
}
