/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IInterceptorAllocator.JoinPointInfo;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link AbstractInstrumentor} represents an abstract method instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractInstrumentor implements IInstrumentor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractInstrumentor.class);
    protected static final String JAVA_LANG_THROWABLE = Type.getInternalName(Throwable.class);
    protected static final Type OBJECT_TYPE = Type.getType(Object.class);
    private final IInterceptorAllocator interceptorAllocator;
    protected final String className;
    protected final String methodName;
    protected final String methodSignature;
    protected final boolean isStatic;
    protected final MethodInstrumentor generator;
    protected final ClassLoader classLoader;
    private final IJoinPointFilter joinPointFilter;
    protected final int overloadNumber;
    private final String sourceFileName;
    private final String sourceDebug;

    public AbstractInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                String methodSignature, int overloadNumber, boolean isStatic, MethodInstrumentor generator, ClassLoader classLoader,
                                IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        Assert.notNull(interceptorAllocator);
        Assert.notNull(className);
        Assert.notNull(methodName);
        Assert.notNull(methodSignature);
        Assert.notNull(generator);

        this.interceptorAllocator = interceptorAllocator;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.isStatic = isStatic;
        this.classLoader = classLoader;
        this.generator = generator;
        this.joinPointFilter = joinPointFilter;
        this.overloadNumber = overloadNumber;
        this.sourceFileName = sourceFileName;
        this.sourceDebug = sourceDebug;
    }

    public boolean isEnterIntercepted() {
        return false;
    }

    public boolean isThrowExitIntercepted() {
        return false;
    }

    public boolean isReturnExitIntercepted() {
        return false;
    }

    public boolean isCallIntercepted() {
        return false;
    }

    public void onEnter() {
    }

    public void onReturnExit(Type returnType) {
    }

    public void onThrowExit() {
    }

    public void onTryCatchBlock(Label label, String catchType) {
    }

    public void onLabel(Label label) {
    }

    public void onMonitorBeforeEnter() {
    }

    public void onMonitorAfterEnter() {
    }

    public void onMonitorBeforeExit() {
    }

    public void onMonitorAfterExit() {
    }

    public void onBeforeCall(int opcode, String owner, String name, String descriptor) {
    }

    public void onAfterCall(int opcode, String owner, String name, String descriptor) {
    }

    public void onThrow() {
    }

    public void onObjectNew(String newInstanceClassName) {
    }

    public void onArrayNew(String elementClassName) {
    }

    public void onBeforeFieldGet(int opcode, String owner, String name, String descriptor) {
    }

    public void onAfterFieldGet(int opcode, String owner, String name, String descriptor) {
    }

    public void onFieldSet(int opcode, String owner, String name, String descriptor) {
    }

    public void onBeforeArrayGet() {
    }

    public void onAfterArrayGet(Type type) {
    }

    public void onArraySet(Type type) {
    }

    public void onLine(int line) {
    }

    protected final JoinPointInfo allocateInterceptor(IJoinPoint.Kind kind, Pointcut pointcut, String calledClassName,
                                                      String calledMemberName, String calledMethodSignature) {
        Assert.notNull(pointcut);

        int classLoaderId = 0;
        if (classLoader != null)
            classLoaderId = classLoader.hashCode();

        int instructionCount = generator.getInstructionCounter().getCount();
        if (pointcut.getMethodFilter() != null && (instructionCount < pointcut.getMethodFilter().getMinInstruction() ||
                instructionCount > pointcut.getMethodFilter().getMaxInstruction()))
            return null;

        JoinPoint joinPoint = new JoinPoint(kind, instructionCount, classLoaderId, className, methodName, methodSignature,
                overloadNumber, pointcut, calledClassName, calledMemberName, calledMethodSignature, sourceFileName, sourceDebug,
                generator.getLastLineNumber());

        boolean logged = false;
        if (pointcut instanceof CatchPointcut || (pointcut instanceof InterceptPointcut &&
                ((InterceptPointcut) pointcut).getKinds().contains(InterceptPointcut.Kind.ENTER)) && !methodName.equals("<init>"))
            generator.addJoinPoint(joinPoint);
        else {
            generator.clearJoinPoints();
            logged = true;
        }

        if (joinPointFilter != null && !joinPointFilter.match(joinPoint))
            return null;

        if (logged && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.joinPointCreated(joinPoint));

        generator.setInstrumented();

        return interceptorAllocator.allocate(classLoader, joinPoint);
    }

    protected final Class getInterceptorClass(Pointcut pointcut) {
        if (pointcut.getInterceptor() instanceof StaticInterceptorConfiguration)
            return ((StaticInterceptorConfiguration) pointcut.getInterceptor()).getInterceptorClass();
        else {
            Assert.isTrue(pointcut.getInterceptor() instanceof DynamicInterceptorConfiguration);
            return Interceptors.class;
        }
    }

    private interface IMessages {
        @DefaultMessage("Join point ''{0}'' is created.")
        ILocalizedMessage joinPointCreated(JoinPoint joinPoint);
    }
}
