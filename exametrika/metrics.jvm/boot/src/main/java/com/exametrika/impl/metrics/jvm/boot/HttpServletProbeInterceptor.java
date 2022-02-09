/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import java.io.BufferedReader;
import java.io.PrintWriter;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link HttpServletProbeInterceptor} represents a static interceptor of HTTP servlet probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HttpServletProbeInterceptor extends StaticInterceptor {
    public static IHttpServletProbeInterceptor interceptor;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }
    }

    public static BufferedReader onReturnExitReader(Object retVal, Object param) {
        if (param == null)
            return (BufferedReader) retVal;

        IHttpServletProbeInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                return interceptor.onReturnExitReader(param, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }

        return (BufferedReader) retVal;
    }

    public static PrintWriter onReturnExitWriter(Object retVal, Object param) {
        if (param == null)
            return (PrintWriter) retVal;

        IHttpServletProbeInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                return interceptor.onReturnExitWriter(param, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }

        return (PrintWriter) retVal;
    }

    public static Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onCallEnter(index, version, instance, callee, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onCallReturnExit(index, version, param, instance, callee, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }
    }

    public static void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = HttpServletProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onCallThrowExit(index, version, param, instance, callee, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            HttpServletProbeInterceptor.interceptor = null;
        }
    }
}
