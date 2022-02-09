/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.profiler.config.StackInterceptPointcut;
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link StackInterceptInstrumentor} represents a method intercept instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackInterceptInstrumentor extends InterceptInstrumentor {
    private final StackInterceptPointcut pointcut;
    private JoinPointInfo info;
    private int param = -1;

    public StackInterceptInstrumentor(StackInterceptPointcut pointcut, IInterceptorAllocator interceptorAllocator,
                                      String className, String methodName, String methodSignature, int overloadNumber, boolean isStatic,
                                      MethodInstrumentor generator, ClassLoader classLoader, IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(pointcut, interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator,
                classLoader, joinPointFilter, sourceFileName, sourceDebug);

        this.pointcut = pointcut;
    }

    @Override
    public boolean isEnterIntercepted() {
        return true;
    }

    @Override
    public boolean isReturnExitIntercepted() {
        return true;
    }

    @Override
    public boolean isThrowExitIntercepted() {
        return true;
    }

    @Override
    public void onEnter() {
        info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
        if (info == null)
            return;

        Type interceptorType = Type.getType(((StaticInterceptorConfiguration) pointcut.getInterceptor()).getInterceptorClass());
        param = generator.newLocal(OBJECT_TYPE);
        generator.push((Type) null);
        generator.storeLocal(param);

        Type methodsType = Type.getType("[Ljava/lang/Object;");
        int methods = generator.newLocal(methodsType);
        generator.getStatic(interceptorType, "methods", Type.getType("[Ljava/lang/Object;"));
        generator.dup();
        generator.storeLocal(methods);

        Label skipEstimate = generator.newLabel();
        Label measure = generator.newLabel();
        generator.ifNonNull(skipEstimate);
        generator.push(info.index);
        generator.invokeStatic(interceptorType, Method.getMethod("boolean onEstimate(int)"));
        generator.ifZCmp(GeneratorAdapter.EQ, measure);
        generator.visitLabel(skipEstimate);

        generator.loadLocal(methods);
        Label skipMeasure = generator.newLabel();
        generator.ifNull(skipMeasure);
        generator.loadLocal(methods);
        generator.push(info.index);
        generator.arrayLoad(methodsType);
        generator.ifNonNull(skipMeasure);

        generator.visitLabel(measure);
        generator.push(info.index);
        generator.push(info.version);
        generator.invokeStatic(interceptorType, Method.getMethod("java.lang.Object onEnter(int,int)"));
        generator.storeLocal(param);

        generator.visitLabel(skipMeasure);
    }

    @Override
    public void onReturnExit(Type returnType) {
        if (info == null)
            return;

        generator.loadLocal(param);
        Label skipMeasure = generator.newLabel();
        generator.ifNull(skipMeasure);

        generator.loadLocal(param);
        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onReturn(java.lang.Object)"));

        generator.visitLabel(skipMeasure);
    }

    @Override
    public void onThrowExit() {
        onReturnExit(null);
    }
}
