/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link ThreadExitPointProbeInterceptor} represents a static interceptor of thread pool executors probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ThreadExitPointProbeInterceptor extends StaticInterceptor {
    public static IThreadExitPointInterceptor interceptor;

    public static Runnable onExecute(int index, int version, Object instance) {
        IThreadExitPointInterceptor interceptor = ThreadExitPointProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onExecute(index, version, instance);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            ThreadExitPointProbeInterceptor.interceptor = null;
        }

        return (Runnable) instance;
    }

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = ThreadExitPointProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            ThreadExitPointProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = ThreadExitPointProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            ThreadExitPointProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = ThreadExitPointProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            ThreadExitPointProbeInterceptor.interceptor = null;
        }
    }
}
