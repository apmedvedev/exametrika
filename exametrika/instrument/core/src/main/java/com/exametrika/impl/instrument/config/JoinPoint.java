/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link JoinPoint} represents an implementation of {@link IJoinPoint}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JoinPoint implements IJoinPoint {
    private final Kind kind;
    private final int id;
    private final int classLoaderId;
    private final String className;
    private final String methodName;
    private final String methodSignature;
    private final int overloadNumber;
    private final Pointcut pointcut;
    private final String calledClassName;
    private final String calledMemberName;
    private final String calledMethodSignature;
    private volatile int lineNumber;
    private final String sourceFileName;
    private final String sourceDebug;

    public JoinPoint(Kind kind, int id, int classLoaderId, String className, String methodName,
                     String methodSignature, int overloadNumber, Pointcut pointcut, String calledClassName, String calledMemberName,
                     String calledMethodSignature, String sourceFileName, String sourceDebug, int lineNumber) {
        Assert.notNull(kind);
        Assert.notNull(className);
        Assert.notNull(methodName);
        Assert.notNull(methodSignature);
        Assert.notNull(pointcut);

        this.kind = kind;
        this.id = id;
        this.classLoaderId = classLoaderId;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.overloadNumber = overloadNumber;
        this.pointcut = pointcut;
        this.calledClassName = calledClassName;
        this.calledMemberName = calledMemberName;
        this.calledMethodSignature = calledMethodSignature;
        this.sourceFileName = sourceFileName;
        this.sourceDebug = sourceDebug;
        this.lineNumber = lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getClassLoaderId() {
        return classLoaderId;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getMethodSignature() {
        return methodSignature;
    }

    @Override
    public int getOverloadNumber() {
        return overloadNumber;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public String getCalledClassName() {
        return calledClassName;
    }

    @Override
    public String getCalledMemberName() {
        return calledMemberName;
    }

    @Override
    public String getCalledMethodSignature() {
        return calledMethodSignature;
    }

    @Override
    public String getSourceFileName() {
        return sourceFileName;
    }

    @Override
    public String getSourceDebug() {
        return sourceDebug;
    }

    @Override
    public int getSourceLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JoinPoint))
            return false;

        JoinPoint joinPoint = (JoinPoint) o;

        return kind == joinPoint.kind && id == joinPoint.id && classLoaderId == joinPoint.classLoaderId &&
                className.equals(joinPoint.className) && methodName.equals(joinPoint.methodName) &&
                pointcut.equals(joinPoint.pointcut);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(kind, id, classLoaderId, className, methodName, pointcut);
    }

    @Override
    public String toString() {
        return "[" + kind + "] " + className + "." + methodSignature + (sourceFileName != null ? ("@" + sourceFileName + ":" + lineNumber) : "");
    }
}
