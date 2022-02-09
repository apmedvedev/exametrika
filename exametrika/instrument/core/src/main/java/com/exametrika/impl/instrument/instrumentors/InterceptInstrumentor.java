/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut.Kind;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link InterceptInstrumentor} represents a method intercept instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class InterceptInstrumentor extends AbstractInstrumentor {
    protected final InterceptPointcut pointcut;
    protected Data context;

    public InterceptInstrumentor(InterceptPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                 String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                 IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public boolean isEnterIntercepted() {
        return pointcut.getKinds().contains(Kind.ENTER);
    }

    @Override
    public boolean isReturnExitIntercepted() {
        return pointcut.getKinds().contains(Kind.RETURN_EXIT);
    }

    @Override
    public boolean isThrowExitIntercepted() {
        return pointcut.getKinds().contains(Kind.THROW_EXIT);
    }

    @Override
    public void onEnter() {
        int localValue = -1;

        if (pointcut.getKinds().contains(InterceptPointcut.Kind.ENTER)) {
            JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
            if (info == null)
                return;

            Data data = new Data();
            data.info = info;
            context = data;

            generator.push(info.index);
            generator.push(info.version);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            if (pointcut.getUseParams()) {
                if (localValue == -1) {
                    generator.loadArgArray();
                    localValue = generator.newLocal(OBJECT_TYPE);
                    generator.storeLocal(localValue);
                }

                generator.loadLocal(localValue);
            } else
                generator.push((Type) null);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "java.lang.Object onEnter(int, int, java.lang.Object, java.lang.Object[])"));

            data.localParam = generator.newLocal(OBJECT_TYPE);
            generator.storeLocal(data.localParam);
        }
    }

    @Override
    public void onReturnExit(Type returnType) {
        int localValue = -1;

        if (pointcut.getKinds().contains(InterceptPointcut.Kind.RETURN_EXIT)) {
            Data data = context;
            if (data == null) {
                data = new Data();
                data.info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
                if (data.info == null)
                    return;
                context = data;
            }

            if (pointcut.getUseParams()) {
                if (localValue == -1 && !returnType.equals(Type.VOID_TYPE)) {
                    if (returnType.equals(Type.LONG_TYPE) || returnType.equals(Type.DOUBLE_TYPE))
                        generator.dup2();
                    else
                        generator.dup();
                    generator.box(returnType);
                    localValue = generator.newLocal(OBJECT_TYPE);
                    generator.storeLocal(localValue);
                }
            }

            generator.push(data.info.index);
            generator.push(data.info.version);

            if (data.localParam != -1)
                generator.loadLocal(data.localParam);
            else
                generator.push((Type) null);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            if (localValue != -1)
                generator.loadLocal(localValue);
            else
                generator.push((Type) null);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onReturnExit(int, int, java.lang.Object, java.lang.Object, java.lang.Object)"));
        }
    }

    @Override
    public void onThrowExit() {
        int localValue = -1;

        if (pointcut.getKinds().contains(InterceptPointcut.Kind.THROW_EXIT)) {
            Data data = context;
            if (data == null) {
                data = new Data();
                data.info = allocateInterceptor(IJoinPoint.Kind.INTERCEPT, pointcut, null, null, null);
                if (data.info == null)
                    return;

                context = data;
            }

            if (localValue == -1) {
                generator.dup();
                localValue = generator.newLocal(OBJECT_TYPE);
                generator.storeLocal(localValue);
            }

            generator.push(data.info.index);
            generator.push(data.info.version);

            if (data.localParam != -1)
                generator.loadLocal(data.localParam);
            else
                generator.push((Type) null);

            if (!isStatic)
                generator.loadThis();
            else
                generator.push((Type) null);

            generator.loadLocal(localValue);

            generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                    "void onThrowExit(int, int, java.lang.Object, java.lang.Object, java.lang.Throwable)"));
        }
    }

    protected static class Data {
        public JoinPointInfo info;
        public int localParam = -1;
    }
}
