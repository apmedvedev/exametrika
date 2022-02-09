/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import java.io.InputStream;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link HttpConnectionProbeInterceptor} represents a static interceptor of HTTP connection probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HttpConnectionProbeInterceptor extends StaticInterceptor {
    public static IHttpConnectionProbeInterceptor interceptor;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = HttpConnectionProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpConnectionProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static InputStream onReturnExit(Object retVal, Object param) {
        if (param == null)
            return (InputStream) retVal;

        IHttpConnectionProbeInterceptor interceptor = HttpConnectionProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                return interceptor.onReturnExit(param, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpConnectionProbeInterceptor.interceptor = null;
        }

        return (InputStream) retVal;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpConnectionProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpConnectionProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpConnectionProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpConnectionProbeInterceptor.interceptor = null;
        }
    }
}
