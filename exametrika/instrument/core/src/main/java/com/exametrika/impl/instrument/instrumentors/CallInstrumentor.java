/**
 * generator. * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import static org.objectweb.asm.Opcodes.ATHROW;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;


/**
 * The {@link CallInstrumentor} represents a method call instrumentor.
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CallInstrumentor extends AbstractInstrumentor {
    private final CallPointcut pointcut;
    private Data context;
    private Label start;
    private int localCallee = -1;
    private boolean needLoadCallee;
    private String calledMethodSignature;

    public CallInstrumentor(CallPointcut pointcut, IInterceptorAllocator interceptorAllocator, String className, String methodName,
                            String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        super(interceptorAllocator, className, methodName, methodSignature, overloadNumber, isStatic, generator, classLoader,
                joinPointFilter, sourceFileName, sourceDebug);

        Assert.notNull(pointcut);

        this.pointcut = pointcut;
    }

    @Override
    public boolean isCallIntercepted() {
        return true;
    }

    @Override
    public void onBeforeCall(int opcode, String owner, String name, String descriptor) {
        localCallee = -1;
        needLoadCallee = false;
        calledMethodSignature = Instrumentors.getMethodSignature(name, descriptor);
        String calledClassName = Type.getObjectType(owner).getClassName();
        Method method = new Method(name, descriptor);
        int localValue = -1;
        boolean found = false;

        if (pointcut.getCalledMethodFilter() != null && !pointcut.getCalledMethodFilter().matchMember(calledClassName, calledMethodSignature))
            return;

        JoinPointInfo info = allocateInterceptor(IJoinPoint.Kind.CALL, pointcut, calledClassName, name, calledMethodSignature);
        if (info == null)
            return;

        found = true;

        if (pointcut.getUseParams()) {
            if (localValue == -1) {
                int[] localArgs = storeArgs(opcode, method);

                loadArgArray(localArgs, method);
                localValue = generator.newLocal(OBJECT_TYPE);
                generator.storeLocal(localValue);
            }
        }

        Data data = new Data();
        data.info = info;
        context = data;

        generator.push(info.index);
        generator.push(info.version);

        if (!isStatic)
            generator.loadThis();
        else
            generator.push((Type) null);

        if (localCallee != -1)
            generator.loadLocal(localCallee);
        else
            generator.push((Type) null);

        if (localValue != -1)
            generator.loadLocal(localValue);
        else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod("java.lang.Object onCallEnter(int, int, java.lang.Object, java.lang.Object, java.lang.Object[])"));

        data.localParam = generator.newLocal(OBJECT_TYPE);
        generator.storeLocal(data.localParam);

        if (found) {
            start = new Label();
            generator.visitLabel(start);
        } else
            start = null;
    }

    @Override
    public void onAfterCall(int opcode, String owner, String name, String descriptor) {
        if (start == null)
            return;

        onAfterReturnCall(opcode, owner, name, descriptor);

        Label proceed = new Label();
        generator.visitJumpInsn(Opcodes.GOTO, proceed);

        Label end = new Label();
        generator.visitLabel(end);

        generator.visitTryCatchBlockNoCache(start, end, end, JAVA_LANG_THROWABLE);
        start = null;

        onAfterThrowCall(opcode, owner, name, descriptor);

        generator.visitInsn(ATHROW);

        generator.visitLabel(proceed);
    }

    private void onAfterReturnCall(int opcode, String owner, String name, String descriptor) {
        if (needLoadCallee) {
            localCallee = generator.newLocal(OBJECT_TYPE);
            generator.storeLocal(localCallee);
        }

        String calledClassName = Type.getObjectType(owner).getClassName();
        Method method = new Method(name, descriptor);
        int localValue = -1;

        if (pointcut.getCalledMethodFilter() != null && !pointcut.getCalledMethodFilter().matchMember(calledClassName, calledMethodSignature))
            return;

        Data data = context;
        if (data == null)
            return;

        if (pointcut.getUseParams()) {
            if (localValue == -1 && !method.getReturnType().equals(Type.VOID_TYPE)) {
                if (method.getReturnType().equals(Type.LONG_TYPE) || method.getReturnType().equals(Type.DOUBLE_TYPE))
                    generator.dup2();
                else
                    generator.dup();
                generator.box(method.getReturnType());
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

        if (localCallee != -1)
            generator.loadLocal(localCallee);
        else
            generator.push((Type) null);

        if (localValue != -1)
            generator.loadLocal(localValue);
        else
            generator.push((Type) null);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                "void onCallReturnExit(int, int, java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)"));
    }

    private void onAfterThrowCall(int opcode, String owner, String name, String descriptor) {
        String calledClassName = Type.getObjectType(owner).getClassName();
        int localValue = -1;

        if (pointcut.getCalledMethodFilter() != null && !pointcut.getCalledMethodFilter().matchMember(calledClassName, calledMethodSignature))
            return;

        Data data = context;
        if (data == null)
            return;

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

        if (!needLoadCallee && localCallee != -1)
            generator.loadLocal(localCallee);
        else
            generator.push((Type) null);

        generator.loadLocal(localValue);

        generator.invokeStatic(Type.getType(getInterceptorClass(pointcut)), Method.getMethod(
                "void onCallThrowExit(int, int, java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Throwable)"));
    }

    private int[] storeArgs(int opcode, Method method) {
        int[] localArgs = new int[method.getArgumentTypes().length];
        for (int i = method.getArgumentTypes().length - 1; i >= 0; i--) {
            Type type = method.getArgumentTypes()[i];
            localArgs[i] = generator.newLocal(type);
            generator.storeLocal(localArgs[i]);
        }

        if (opcode != Opcodes.INVOKESTATIC) {
            generator.dup();

            if (!method.getName().equals("<init>")) {
                localCallee = generator.newLocal(OBJECT_TYPE);
                generator.storeLocal(localCallee);
            } else
                needLoadCallee = true;
        }

        for (int i = 0; i < method.getArgumentTypes().length; i++)
            generator.loadLocal(localArgs[i]);

        return localArgs;
    }

    private void loadArgArray(int[] localArgs, Method method) {
        generator.push(localArgs.length);
        generator.newArray(OBJECT_TYPE);
        for (int i = 0; i < localArgs.length; i++) {
            generator.dup();
            generator.push(i);
            generator.loadLocal(localArgs[i]);
            generator.box(method.getArgumentTypes()[i]);
            generator.arrayStore(OBJECT_TYPE);
        }
    }

    private static class Data {
        JoinPointInfo info;
        int localParam = -1;
    }
}
