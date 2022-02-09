/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link UdpProbeInterceptor} represents a static interceptor of UDP socket probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class UdpProbeInterceptor extends StaticInterceptor {
    public static IInterceptor interceptor;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        IInterceptor interceptor = UdpProbeInterceptor.interceptor;

        try {
            if (interceptor != null && !interceptor.isSuspended())
                return interceptor.onEnter(index, version, instance, params);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            UdpProbeInterceptor.interceptor = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (param == null)
            return;

        IInterceptor interceptor = UdpProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onReturnExit(index, version, param, instance, retVal);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            UdpProbeInterceptor.interceptor = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (param == null)
            return;

        IInterceptor interceptor = UdpProbeInterceptor.interceptor;

        try {
            if (interceptor != null)
                interceptor.onThrowExit(index, version, param, instance, exception);
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            UdpProbeInterceptor.interceptor = null;
        }
    }
}
