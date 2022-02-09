/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;


/**
 * The {@link MethodInstrumentor} represents a method instrumentor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MethodInstrumentor extends MethodInstrumentorAdapter implements IMethodInstrumentor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractInstrumentor.class);
    private final IInterceptorAllocator interceptorAllocator;
    private Set<String> superTypes;
    private Set<String> classAnnotations;
    private final ClassLoader classLoader;
    private boolean isStatic;
    private final Type returnType;
    private LinkedList<AbstractInstrumentor> instrumentors = new LinkedList<AbstractInstrumentor>();
    private IInstructionCounter instructionCounter;
    private CodeSizeEvaluator codeSizeEvaluator;
    private final IJoinPointFilter joinPointFilter;
    private final ClassInstrumentor classInstrumentor;
    private List<JoinPoint> joinPoints;
    private boolean instrumented;
    private final int overloadNumber;
    private final String sourceFileName;
    private final String sourceDebug;

    public MethodInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String superName, Set<String> superTypes, Set<String> classAnnotations,
                              String methodName, String methodSignature, int access, String desc, MethodVisitor mv, Set<Pointcut> pointcuts, ClassLoader classLoader,
                              Class clazz, IJoinPointFilter joinPointFilter, ClassInstrumentor classInstrumentor, CodeSizeEvaluator codeSizeEvaluator,
                              int overloadNumber, String sourceFileName, String sourceDebug) {
        super(className, superName, methodName, methodSignature, access, desc, mv, pointcuts, clazz);

        Assert.notNull(interceptorAllocator);

        this.interceptorAllocator = interceptorAllocator;
        this.classLoader = classLoader;
        this.joinPointFilter = joinPointFilter;
        this.classInstrumentor = classInstrumentor;
        isStatic = (access & ACC_STATIC) == ACC_STATIC;
        returnType = Type.getReturnType(desc);
        this.superTypes = superTypes;
        this.classAnnotations = classAnnotations;
        this.codeSizeEvaluator = codeSizeEvaluator;
        this.overloadNumber = overloadNumber;
        this.sourceFileName = sourceFileName;
        this.sourceDebug = sourceDebug;
    }

    public IInstructionCounter getInstructionCounter() {
        return instructionCounter;
    }

    public void setInstructionCounter(IInstructionCounter instructionCounter) {
        Assert.notNull(instructionCounter);
        this.instructionCounter = instructionCounter;
    }

    public void addJoinPoint(JoinPoint joinPoint) {
        if (joinPoints == null)
            joinPoints = new ArrayList<JoinPoint>();
        this.joinPoints.add(joinPoint);
    }

    public void clearJoinPoints() {
        if (joinPoints != null)
            joinPoints.clear();
    }

    public void setInstrumented() {
        instrumented = true;
    }

    @Override
    public void visitCode() {
        pointcuts = matchMethod(className, superTypes, classAnnotations, methodSignature, annotations, pointcuts);

        if (pointcuts == null || pointcuts.isEmpty())
            disabled = true;

        if (!disabled) {
            for (Pointcut pointcut : pointcuts)
                instrumentors.add((AbstractInstrumentor) pointcut.createInstrumentor(interceptorAllocator, className, methodName,
                        methodSignature, overloadNumber, isStatic, this, classLoader, joinPointFilter, sourceFileName, sourceDebug));
        }

        super.visitCode();
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        boolean instrumentationError = false;
        if (codeSizeEvaluator != null && codeSizeEvaluator.getMaxSize() > 65535) {
            instrumentationError = true;
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.methodTooBig(className + '.' + methodSignature));
        }
        if (instrumented && classInstrumentor != null)
            classInstrumentor.onMethodInstrumented(methodSignature, instrumentationError);
    }

    @Override
    protected boolean isReturnExitIntercepted() {
        for (AbstractInstrumentor instrumentor : instrumentors) {
            if (instrumentor.isReturnExitIntercepted())
                return true;
        }
        return false;
    }

    @Override
    protected boolean isThrowExitIntercepted() {
        for (AbstractInstrumentor instrumentor : instrumentors) {
            if (instrumentor.isThrowExitIntercepted())
                return true;
        }
        return false;
    }

    @Override
    protected boolean isCallIntercepted() {
        for (AbstractInstrumentor instrumentor : instrumentors) {
            if (instrumentor.isCallIntercepted())
                return true;
        }
        return false;
    }

    @Override
    protected void onEnter() {
        for (AbstractInstrumentor instrumentor : instrumentors) {
            if (instrumentor.isEnterIntercepted())
                instrumentor.onEnter();
        }
    }

    @Override
    protected void onReturnExit(boolean hasRetVal) {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            if (instrumentor.isReturnExitIntercepted())
                instrumentor.onReturnExit(returnType);
        }
    }

    @Override
    protected void onThrowExit() {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            if (instrumentor.isThrowExitIntercepted())
                instrumentor.onThrowExit();
        }
    }

    @Override
    protected void onTryCatchBlock(Label handler, String catchType) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onTryCatchBlock(handler, catchType);
    }

    @Override
    protected void onLabel(Label label) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onLabel(label);
    }

    @Override
    protected void onMonitorBeforeEnter() {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onMonitorBeforeEnter();
    }

    @Override
    protected void onMonitorAfterEnter() {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onMonitorAfterEnter();
    }

    @Override
    protected void onMonitorBeforeExit() {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            instrumentor.onMonitorBeforeExit();
        }
    }

    @Override
    protected void onMonitorAfterExit() {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            instrumentor.onMonitorAfterExit();
        }
    }

    @Override
    protected void onBeforeCall(int opcode, String owner, String name, String descriptor) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onBeforeCall(opcode, owner, name, descriptor);
    }

    @Override
    protected void onAfterCall(int opcode, String owner, String name, String descriptor) {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            instrumentor.onAfterCall(opcode, owner, name, descriptor);
        }
    }

    @Override
    protected void onThrow() {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onThrow();
    }

    @Override
    protected void onObjectNew(String newInstanceClassName) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onObjectNew(newInstanceClassName);
    }

    @Override
    protected void onArrayNew(String elementClassName) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onArrayNew(elementClassName);
    }

    @Override
    protected void onBeforeFieldGet(int opcode, String owner, String name, String descriptor) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onBeforeFieldGet(opcode, owner, name, descriptor);
    }

    @Override
    protected void onAfterFieldGet(int opcode, String owner, String name, String descriptor) {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            instrumentor.onAfterFieldGet(opcode, owner, name, descriptor);
        }
    }

    @Override
    protected void onFieldSet(int opcode, String owner, String name, String descriptor) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onFieldSet(opcode, owner, name, descriptor);
    }

    @Override
    protected void onBeforeArrayGet() {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onBeforeArrayGet();
    }

    @Override
    protected void onAfterArrayGet(Type type) {
        for (Iterator<AbstractInstrumentor> it = instrumentors.descendingIterator(); it.hasNext(); ) {
            AbstractInstrumentor instrumentor = it.next();
            instrumentor.onAfterArrayGet(type);
        }
    }

    @Override
    protected void onArraySet(Type type) {
        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onArraySet(type);
    }

    @Override
    protected void onLine(int line) {
        if (joinPoints != null) {
            for (JoinPoint joinPoint : joinPoints) {
                joinPoint.setLineNumber(line);

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.joinPointCreated(joinPoint));
            }

            joinPoints.clear();
        }

        for (AbstractInstrumentor instrumentor : instrumentors)
            instrumentor.onLine(line);
    }

    private Set<Pointcut> matchMethod(String className, Set<String> superTypes, Set<String> classAnnotations, String methodSignature,
                                      Set<String> methodAnnotations, Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = new TreeSet<Pointcut>(new Comparator<Pointcut>() {
            @Override
            public int compare(Pointcut o1, Pointcut o2) {
                if (o1.getPriority() < o2.getPriority())
                    return 1;
                else if (o1.getPriority() == o2.getPriority())
                    return o1.getName().compareTo(o2.getName());
                else
                    return -1;
            }
        });

        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() == null || pointcut.getMethodFilter().matchMethod(className, superTypes,
                    classAnnotations, methodSignature, methodAnnotations))
                matched.add(pointcut);
        }

        return matched;
    }

    private interface IMessages {
        @DefaultMessage("Join point ''{0}'' is created.")
        ILocalizedMessage joinPointCreated(JoinPoint joinPoint);

        @DefaultMessage("Could not instrument method ''{0}''. Method size exceeds maximal method size of 64k.")
        ILocalizedMessage methodTooBig(String methodName);
    }
}
