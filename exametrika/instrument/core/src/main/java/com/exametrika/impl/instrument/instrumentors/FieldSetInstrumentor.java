/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import static org.objectweb.asm.Opcodes.PUTFIELD;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link FieldSetInstrumentor} represents a method field set instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldSetInstrumentor extends AbstractInstrumentor {
    private final FieldSetPointcut pointcut;

    public FieldSetInstrumentor(FieldSetPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public void onFieldSet(int opcode, String owner, String name, String descriptor) {
        String fieldClassName = Type.getObjectType(owner).getClassName();
        Type fieldType = Type.getType(descriptor);
        int localValue = -1;
        int localField = -1;

        if (pointcut.getFieldFilter() != null && !pointcut.getFieldFilter().matchMember(fieldClassName, name))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.FIELD_SET, pointcut, fieldClassName, name, null);
        if (info == null)
            return;

        if (pointcut.getUseParams()) {
            if (localValue == -1) {
                if (fieldType.equals(Type.LONG_TYPE) || fieldType.equals(Type.DOUBLE_TYPE))
                    generator.dup2();
                else
                    generator.dup();

                localValue = generator.newLocal(fieldType);
                generator.storeLocal(localValue);

                if (opcode == PUTFIELD) {
                    if (fieldType.equals(Type.LONG_TYPE) || fieldType.equals(Type.DOUBLE_TYPE))
                        generator.pop2();
                    else
                        generator.pop();

                    localField = generator.newLocal(OBJECT_TYPE);
                    generator.dup();
                    generator.storeLocal(localField);

                    generator.loadLocal(localValue);
                }
            }
        }

        generator.push(info.index);
        generator.push(info.version);

        if (!isStatic)
            generator.loadThis();
        else
            generator.push((Type) null);

        if (opcode == PUTFIELD && localField != -1)
            generator.loadLocal(localField);
        else
            generator.push((Type) null);

        if (localValue != -1) {
            generator.loadLocal(localValue);
            generator.box(fieldType);
        } else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("void onFieldSet(int, int, java.lang.Object, java.lang.Object, java.lang.Object)"));
    }
}
