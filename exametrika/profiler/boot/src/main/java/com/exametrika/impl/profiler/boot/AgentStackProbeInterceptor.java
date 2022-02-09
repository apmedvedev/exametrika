/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.profiler.boot.ThreadLocalContainer;


/**
 * The {@link AgentStackProbeInterceptor} represents a agent-based stack probe interceptor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AgentStackProbeInterceptor extends StackProbeInterceptor {
    public static boolean onEstimate(int index) {
        IStackInterceptor interceptor = StackProbeInterceptor.estimationInterceptor;

        try {
            if (interceptor != null) {
                ThreadLocalContainer container = (ThreadLocalContainer) Thread.currentThread()._exaTls;
                if (container != null) {
                    long[] methodCounters = container.methodCounters;
                    if (methodCounters != null)
                        methodCounters[index]++;

                    return true;
                } else
                    return false;
            }
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            StackProbeInterceptor.estimationInterceptor = null;
        }

        return true;
    }

    public static Object onEnter(int index, int version) {
        IStackInterceptor interceptor = StackProbeInterceptor.interceptor;

        try {
            if (interceptor != null) {
                ThreadLocalContainer container = (ThreadLocalContainer) Thread.currentThread()._exaTls;
                if (container != null) {
                    if (container.inCall || container.top == null || container.top.blocked == null)
                        return null;
                    else if (container.suspended) {
                        container.inCall = true;
                        return container;
                    }
                }

                return interceptor.onEnter(index, version);
            }
        } catch (Throwable e) {
            if (interceptor != null)
                interceptor.logError(e);

            StackProbeInterceptor.interceptor = null;
        }

        return null;
    }
}
