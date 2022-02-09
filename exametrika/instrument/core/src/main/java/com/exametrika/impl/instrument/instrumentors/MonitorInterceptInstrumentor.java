/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link MonitorInterceptInstrumentor} represents a method monitor intercept instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MonitorInterceptInstrumentor extends AbstractInstrumentor {
    private final MonitorInterceptPointcut pointcut;
    private int localMonitor;

    public MonitorInterceptInstrumentor(MonitorInterceptPointcut pointcut, IInterceptorAllocator interceptorAllocator,
                                        String className, String methodName, String methodSignature, int overloadNumber, boolean isStatic,
                                        MethodInstrumentor generator, ClassLoader classLoader, IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onMonitorBeforeEnter() {
        localMonitor = -1;

        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.BEFORE_ENTER) ||
                pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.AFTER_ENTER)) {
            generator.dup();
            localMonitor = generator.newLocal(OBJECT_TYPE);
            generator.storeLocal(localMonitor);
        }

        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.BEFORE_ENTER)) {
            JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.BEFORE_MONITOR_ENTER, pointcut, null, null, null);
            if (info == null)
                return;

            generator.push(info.index);
            generator.push(info.version);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            generator.loadLocal(localMonitor);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onMonitorBeforeEnter(int, int, java.lang.Object, java.lang.Object)"));
        }
    }

    @Override
    public void onMonitorAfterEnter() {
        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.AFTER_ENTER)) {
            JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.AFTER_MONITOR_ENTER, pointcut, null, null, null);
            if (info == null)
                return;

            generator.push(info.index);
            generator.push(info.version);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            generator.loadLocal(localMonitor);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onMonitorAfterEnter(int, int, java.lang.Object, java.lang.Object)"));
        }
    }

    @Override
    public void onMonitorBeforeExit() {
        localMonitor = -1;

        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.BEFORE_EXIT) ||
                pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.AFTER_EXIT)) {
            generator.dup();
            localMonitor = generator.newLocal(OBJECT_TYPE);
            generator.storeLocal(localMonitor);
        }

        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.BEFORE_EXIT)) {
            JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.BEFORE_MONITOR_EXIT, pointcut, null, null, null);
            if (info == null)
                return;

            generator.push(info.index);
            generator.push(info.version);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            generator.loadLocal(localMonitor);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onMonitorBeforeExit(int, int, java.lang.Object, java.lang.Object)"));
        }
    }

    @Override
    public void onMonitorAfterExit() {
        if (pointcut.getKinds().contains(MonitorInterceptPointcut.Kind.AFTER_EXIT)) {
            JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.AFTER_MONITOR_EXIT, pointcut, null, null, null);
            if (info == null)
                return;

            generator.push(info.index);
            generator.push(info.version);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            generator.loadLocal(localMonitor);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onMonitorAfterExit(int, int, java.lang.Object, java.lang.Object)"));
        }
    }
}
