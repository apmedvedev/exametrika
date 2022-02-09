/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.profiler.config.ThreadExitPointInterceptPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link ThreadInterceptInstrumentor} represents a method intercept instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ThreadInterceptInstrumentor extends InterceptInstrumentor {
    private final ThreadExitPointInterceptPointcut pointcut;

    public ThreadInterceptInstrumentor(ThreadExitPointInterceptPointcut pointcut, IInterceptorAllocator interceptorAllocator,
                                       String className, String methodName, String methodSignature, int overloadNumber, boolean isStatic,
                                       MethodInstrumentor generator, ClassLoader classLoader, IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(pointcut, interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator,
                classLoader, joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onEnter() {
        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
        if (info == null)
            return;

        generator.push(info.index);
        generator.push(info.version);
        generator.loadArg(0);
        generator.invokeStatic(Type.getType(((StaticInterceptorConfiguration) pointcut.getInterceptor()).getInterceptorClass()),
                Method.getMethod("java.lang.Runnable onExecute(int, int, java.lang.Object)"));
        generator.storeArg(0);
    }
}
