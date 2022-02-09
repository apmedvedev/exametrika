/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link ArraySetInstrumentor} represents a method array set instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ArraySetInstrumentor extends AbstractInstrumentor {
    private final ArraySetPointcut pointcut;

    public ArraySetInstrumentor(ArraySetPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onArraySet(Type type) {
        int localValue = -1;
        int localArray = -1;
        int localIndex = -1;
        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.ARRAY_SET, pointcut, null, null, null);
        if (info == null)
            return;

        if (pointcut.getUseParams()) {
            if (localValue == -1) {
                localValue = generator.newLocal(type);
                generator.storeLocal(localValue);

                localArray = generator.newLocal(OBJECT_TYPE);
                localIndex = generator.newLocal(Type.INT_TYPE);
                generator.dup2();
                generator.storeLocal(localIndex);
                generator.storeLocal(localArray);

                generator.loadLocal(localValue);
            }
        }

        generator.push(info.index);
        generator.push(info.version);

        if (!isStatic)
            generator.loadThis();
        else
            generator.push((Type) null);

        if (localArray != -1)
            generator.loadLocal(localArray);
        else
            generator.push((Type) null);

        if (localIndex != -1)
            generator.loadLocal(localIndex);
        else
            generator.push(-1);

        if (localValue != -1) {
            generator.loadLocal(localValue);
            generator.box(type);
        } else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onArraySet(int, int, java.lang.Object, java.lang.Object, int, java.lang.Object)"));
    }
}
