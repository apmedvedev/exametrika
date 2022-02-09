/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Out;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.spi.instrument.IInterceptorAllocator;


/**
 * The {@link ClassInstrumentor} represents a class instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ClassInstrumentor extends ClassVisitor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ClassInstrumentor.class);
    private final IInterceptorAllocator interceptorAllocator;
    private final ClassLoader classLoader;
    private Set<Pointcut> pointcuts;
    private final Class clazz;
    private String className;
    private String superName;
    private Set<String> superTypes = new LinkedHashSet<String>();
    private Set<String> annotations = new LinkedHashSet<String>();
    private final IJoinPointFilter joinPointFilter;
    private boolean methodsInstrumented;
    private boolean firstMethod = true;
    private Set<String> errorMethods;
    private Map<String, Out<Integer>> overloadedMethodCounts = new HashMap<String, Out<Integer>>();
    private String sourceFileName;
    private String sourceDebug;

    public ClassInstrumentor(ClassVisitor cv, IInterceptorAllocator interceptorAllocator, ClassLoader classLoader, Set<Pointcut> pointcuts, Class clazz,
                             IJoinPointFilter joinPointFilter, Set<String> errorMethods) {
        super(Opcodes.ASM5, cv);

        Assert.notNull(interceptorAllocator);
        Assert.notNull(pointcuts);
        Assert.isTrue(!pointcuts.isEmpty());
        Assert.notNull(errorMethods);

        this.interceptorAllocator = interceptorAllocator;
        this.classLoader = classLoader;
        this.pointcuts = pointcuts;
        this.clazz = clazz;
        this.joinPointFilter = joinPointFilter;
        this.errorMethods = errorMethods;
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getErrorMethods() {
        return errorMethods;
    }

    public String getSuperName() {
        return superName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ClassLoader classLoader = this.classLoader;

        if (clazz == null) {
            this.className = Type.getObjectType(name).getClassName();

            if (superName != null)
                this.superName = Type.getObjectType(superName).getClassName();

            if ((access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE)
                throw new SkipInstrumentationException();

            try {
                if (superName != null)
                    Instrumentors.addSuperTypes(this.superTypes, superName, true, classLoader);

                if (interfaces != null && interfaces.length > 0) {
                    for (int i = 0; i < interfaces.length; i++)
                        Instrumentors.addSuperTypes(this.superTypes, interfaces[i], true, classLoader);
                }
            } catch (InstrumentationException e) {
                logger.log(LogLevel.ERROR, e);

                throw new SkipInstrumentationException();
            }
        } else {
            className = clazz.getName();
            Instrumentors.addSuperTypes(this.superTypes, clazz, false);

            if (clazz.getSuperclass() != null)
                this.superName = clazz.getSuperclass().getName();

            for (Annotation annotation : clazz.getAnnotations())
                this.annotations.add(annotation.annotationType().getName());
        }

        if (interceptorAllocator instanceof IInterceptorManager) {
            IInterceptorManager interceptorManager = (IInterceptorManager) interceptorAllocator;
            interceptorManager.free(classLoader, className);
        }

        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        if (!methodsInstrumented)
            throw new SkipInstrumentationException();

        cv.visitEnd();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (clazz == null)
            annotations.add(Type.getType(desc).getClassName());

        return cv.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Out<Integer> count = overloadedMethodCounts.get(name);
        if (count == null) {
            count = new Out<Integer>(0);
            overloadedMethodCounts.put(name, count);
        }

        int overloadNumber = count.value++;

        if (clazz == null && firstMethod) {
            firstMethod = false;
            this.pointcuts = matchClass(className, superTypes, annotations, this.pointcuts);

            if (this.pointcuts == null || this.pointcuts.isEmpty())
                throw new SkipInstrumentationException();
        }

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        if (((access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) ||
                ((access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE) ||
                ((access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC) ||
                ((access & Opcodes.ACC_BRIDGE) == Opcodes.ACC_BRIDGE) ||
                name.equals("<clinit>"))
            return mv;

        String methodSignature = Instrumentors.getMethodSignature(name, desc);

        if (errorMethods.contains(methodSignature))
            return mv;

        Set<Pointcut> pointcuts = matchMethod(className, superTypes, annotations, methodSignature, this.pointcuts);

        if (pointcuts == null || pointcuts.isEmpty())
            return mv;
        else {
            CodeSizeEvaluator codeSizeEvaluator = new CodeSizeEvaluator(mv);
            MethodInstrumentor instrumentor = new MethodInstrumentor(interceptorAllocator, className, superName,
                    superTypes, annotations, name, methodSignature, access, desc, codeSizeEvaluator, pointcuts,
                    classLoader, clazz, joinPointFilter, this, codeSizeEvaluator, overloadNumber, sourceFileName, sourceDebug);
            InstructionCounter instructionCounter = new InstructionCounter(instrumentor);
            instrumentor.setInstructionCounter(instructionCounter);

            return instructionCounter;
        }
    }

    @Override
    public void visitSource(String source, String debug) {
        sourceFileName = source;
        sourceDebug = debug;
        cv.visitSource(source, debug);
    }

    public void onMethodInstrumented(String methodSignature, boolean instrumentationError) {
        if (instrumentationError) {
            errorMethods.add(methodSignature);
            return;
        }

        if (!methodsInstrumented) {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.classInstrumented(className));

            methodsInstrumented = true;
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.methodInstrumented(className, methodSignature));
    }

    private Set<Pointcut> matchClass(String className, Set<String> superTypes, Set<String> annotations, Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = new LinkedHashSet<Pointcut>();
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() != null && pointcut.getMethodFilter().notMatchClass(className, superTypes, annotations))
                continue;

            matched.add(pointcut);
        }

        return matched;
    }

    private Set<Pointcut> matchMethod(String className, Set<String> superTypes, Set<String> annotations, String methodName,
                                      Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = null;
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() != null && pointcut.getMethodFilter().notMatchMethod(className, superTypes, annotations, methodName))
                continue;

            if (matched == null)
                matched = new LinkedHashSet<Pointcut>();

            matched.add(pointcut);
        }

        return matched;
    }

    private interface IMessages {
        @DefaultMessage("Class ''{0}'' is instrumented.")
        ILocalizedMessage classInstrumented(String className);

        @DefaultMessage("Method ''{0}.{1}'' is instrumented.")
        ILocalizedMessage methodInstrumented(String className, String methodName);
    }
}
