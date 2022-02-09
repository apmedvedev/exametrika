/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link AllocationProbeInterceptor} represents a static interceptor of allocation probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllocationProbeInterceptor extends StaticInterceptor {
    public static IInterceptor interceptor;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = AllocationProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            AllocationProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = AllocationProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            AllocationProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = AllocationProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            AllocationProbeInterceptor.interceptor = null;
        }
    }

    public static void onNewObject(int index, int version, Object instance, Object object) {
        IInterceptor interceptor = AllocationProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                interceptor.onNewObject(index, version, instance, object);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            AllocationProbeInterceptor.interceptor = null;
        }
    }

    public static void onNewArray(int index, int version, Object instance, Object array) {
        IInterceptor interceptor = AllocationProbeInterceptor.interceptor;

        try {

            if (interceptor != null && !interceptor.isSuspended())
                interceptor.onNewArray(index, version, instance, array);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            AllocationProbeInterceptor.interceptor = null;
        }
    }
}
