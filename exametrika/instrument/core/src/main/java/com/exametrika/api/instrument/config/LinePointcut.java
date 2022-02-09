/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.LineInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link LinePointcut} represents a line pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LinePointcut extends Pointcut {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int startLine;
    private final int endLine;

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param interceptor  interceptor
     * @param startLine    start line
     * @param endLine      end line
     * @param singleton    if true pointcut generates single join point
     */
    public LinePointcut(String name, QualifiedMethodFilter methodFilter,
                        InterceptorConfiguration interceptor, int startLine, int endLine, boolean singleton) {
        super(name, methodFilter, interceptor, singleton, 0);

        this.startLine = startLine;
        this.endLine = endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new LineInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LinePointcut))
            return false;

        LinePointcut pointcut = (LinePointcut) o;
        return super.equals(o) && startLine == pointcut.startLine && endLine == pointcut.endLine;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(startLine, endLine);
    }

    @Override
    public String toString() {
        return super.toString() + messages.toString(startLine, endLine).toString();
    }

    private interface IMessages {
        @DefaultMessage(", lines: [{0}..{1}]")
        ILocalizedMessage toString(int startLine, int endLine);
    }
}
