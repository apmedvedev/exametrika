/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import java.lang.reflect.Field;

import com.exametrika.spi.profiler.boot.ThreadLocalContainer;

import sun.misc.Unsafe;


/**
 * The {@link AgentlessStackProbeInterceptor} represents a agentless stack probe interceptor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AgentlessStackProbeInterceptor extends StackProbeInterceptor {
    private static final Unsafe unsafe;
    private static final long offset;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);

            field = Thread.class.getDeclaredField("target");
            offset = unsafe.objectFieldOffset(field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean onEstimate(int index) {
        IStackInterceptor interceptor = StackProbeInterceptor.estimationInterceptor;

        try {
            if (interceptor != null) {
                Runnable runnable = (Runnable) unsafe.getObject(Thread.currentThread(), offset);
                if (runnable instanceof ThreadLocalContainer) {
                    ThreadLocalContainer container = (ThreadLocalContainer) runnable;
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
                Runnable runnable = (Runnable) unsafe.getObject(Thread.currentThread(), offset);
                if (runnable instanceof ThreadLocalContainer) {
                    ThreadLocalContainer container = (ThreadLocalContainer) runnable;
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
