/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.metrics.jvm.config.HttpInterceptPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link HttpInterceptInstrumentor} represents a method intercept instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HttpInterceptInstrumentor extends InterceptInstrumentor {
    private final HttpInterceptPointcut pointcut;
    private final String interceptorMethodSignature;

    public HttpInterceptInstrumentor(HttpInterceptPointcut pointcut,
                                     IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                     String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                     IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug, String interceptorMethodSignature) {
        super(pointcut, interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic,
                generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);
        Assert.notNull(interceptorMethodSignature);

        this.pointcut = pointcut;
        this.interceptorMethodSignature = interceptorMethodSignature;
    }

    @Override
    public void onReturnExit(Type returnType) {
        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
        if (info == null)
            return;

        if (context != null)
            generator.loadLocal(context.localParam);
        else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(((StaticInterceptorConfiguration) pointcut.getInterceptor()).getInterceptorClass()),
                Method.getMethod(interceptorMethodSignature));
    }
}
