/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link JmsConsumerProbeInterceptor} represents a static interceptor of JMS consumer probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JmsConsumerProbeInterceptor extends StaticInterceptor {
    public static IInterceptor interceptor;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }
    }

    public static Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onCallEnter(index, version, instance, callee, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onCallReturnExit(index, version, param, instance, callee, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }
    }

    public static void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = JmsConsumerProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onCallThrowExit(index, version, param, instance, callee, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            JmsConsumerProbeInterceptor.interceptor = null;
        }
    }
}
