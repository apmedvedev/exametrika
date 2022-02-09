/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link ArrayGetInstrumentor} represents a method array get instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ArrayGetInstrumentor extends AbstractInstrumentor {
    private final ArrayGetPointcut pointcut;
    private int localArray;
    private int localIndex;

    public ArrayGetInstrumentor(ArrayGetPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onBeforeArrayGet() {
        localArray = -1;
        localIndex = -1;

        if (pointcut.getUseParams()) {
            localArray = generator.newLocal(OBJECT_TYPE);
            localIndex = generator.newLocal(Type.INT_TYPE);
            generator.dup2();
            generator.storeLocal(localIndex);
            generator.storeLocal(localArray);
        }
    }

    @Override
    public void onAfterArrayGet(Type type) {
        int localValue = -1;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.ARRAY_GET, pointcut, null, null, null);
        if (info == null)
            return;

        if (pointcut.getUseParams()) {
            if (localValue == -1) {
                if (type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE))
                    generator.dup2();
                else
                    generator.dup();
                generator.box(type);
                localValue = generator.newLocal(OBJECT_TYPE);
                generator.storeLocal(localValue);
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

        if (localValue != -1)
            generator.loadLocal(localValue);
        else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onArrayGet(int, int, java.lang.Object, java.lang.Object, int, java.lang.Object)"));
    }
}
