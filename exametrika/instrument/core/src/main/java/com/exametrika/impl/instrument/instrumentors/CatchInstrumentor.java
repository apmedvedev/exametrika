/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link CatchInstrumentor} represents a method catch instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CatchInstrumentor extends AbstractInstrumentor {
    private final CatchPointcut pointcut;
    private Map<Label, String> catchHandlers = new LinkedHashMap<Label, String>();

    public CatchInstrumentor(CatchPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                             String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                             IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onTryCatchBlock(Label handler, String catchType) {
        if (catchType != null)
            catchHandlers.put(handler, catchType);
    }

    @Override
    public void onLabel(Label label) {
        String catchType = catchHandlers.get(label);
        if (catchType == null)
            return;

        String exceptionClassName = Type.getObjectType(catchType).getClassName();

        if (pointcut.getExceptionClassFilter() != null && !pointcut.getExceptionClassFilter().matchClass(exceptionClassName))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.CATCH, pointcut, exceptionClassName, null, null);
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

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onCatch(int, int, java.lang.Object, java.lang.Throwable)"));
    }
}
