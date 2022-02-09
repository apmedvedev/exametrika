/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link NewArrayInstrumentor} represents a method new array instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class NewArrayInstrumentor extends AbstractInstrumentor {
    private final NewArrayPointcut pointcut;

    public NewArrayInstrumentor(NewArrayPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onArrayNew(String elementClassName) {
        if (pointcut.getElementClassFilter() != null && !pointcut.getElementClassFilter().matchClass(elementClassName))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.NEW_ARRAY, pointcut, elementClassName, null, null);
        if (info == null)
            return;

        generator.dup();
        int local = generator.newLocal(OBJECT_TYPE);
        generator.storeLocal(local);

        generator.push(info.index);
        generator.push(info.version);

        if (!isStatic)
            generator.loadThis();
        else
            generator.push((Type) null);

        generator.loadLocal(local);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onNewArray(int, int, java.lang.Object, java.lang.Object)"));
    }
}
