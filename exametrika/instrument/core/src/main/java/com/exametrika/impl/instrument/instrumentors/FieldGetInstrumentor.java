/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import static org.objectweb.asm.Opcodes.GETSTATIC;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link FieldGetInstrumentor} represents a method field get instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldGetInstrumentor extends AbstractInstrumentor {
    private final FieldGetPointcut pointcut;
    private int localField;

    public FieldGetInstrumentor(FieldGetPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onBeforeFieldGet(int opcode, String owner, String name, String descriptor) {
        if (opcode == GETSTATIC)
            return;

        localField = -1;
        String fieldClassName = Type.getObjectType(owner).getClassName();

        if (pointcut.getUseParams() && (pointcut.getFieldFilter() == null || pointcut.getFieldFilter().matchMember(fieldClassName, name))) {
            localField = generator.newLocal(OBJECT_TYPE);
            generator.dup();
            generator.storeLocal(localField);
        }
    }

    @Override
    public void onAfterFieldGet(int opcode, String owner, String name, String descriptor) {
        String fieldClassName = Type.getObjectType(owner).getClassName();
        Type fieldType = Type.getType(descriptor);

        int localValue = -1;
        if (pointcut.getFieldFilter() != null && !pointcut.getFieldFilter().matchMember(fieldClassName, name))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.FIELD_GET, pointcut, fieldClassName, name, null);
        if (info == null)
            return;

        if (pointcut.getUseParams()) {
            if (localValue == -1) {
                if (fieldType.equals(Type.LONG_TYPE) || fieldType.equals(Type.DOUBLE_TYPE))
                    generator.dup2();
                else
                    generator.dup();
                generator.box(fieldType);
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

        if (opcode != GETSTATIC && localField != -1)
            generator.loadLocal(localField);
        else
            generator.push((Type) null);

        if (localValue != -1)
            generator.loadLocal(localValue);
        else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onFieldGet(int, int, java.lang.Object, java.lang.Object, java.lang.Object)"));
    }
}
