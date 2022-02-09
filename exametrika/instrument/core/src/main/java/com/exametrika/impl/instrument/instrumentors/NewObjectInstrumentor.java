/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link NewObjectInstrumentor} represents a method new object instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class NewObjectInstrumentor extends AbstractInstrumentor {
    private final NewObjectPointcut pointcut;

    public NewObjectInstrumentor(NewObjectPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                 String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                 IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onObjectNew(String newInstanceClassName) {
        if (pointcut.getNewInstanceClassFilter() != null && !pointcut.getNewInstanceClassFilter().matchClass(newInstanceClassName))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.NEW_OBJECT, pointcut, newInstanceClassName, null, null);
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

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onNewObject(int, int, java.lang.Object, java.lang.Object)"));
    }
}
