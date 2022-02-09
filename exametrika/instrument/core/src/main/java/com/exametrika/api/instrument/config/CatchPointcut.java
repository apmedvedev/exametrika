/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.CatchInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link CatchPointcut} represents a catch pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CatchPointcut extends Pointcut {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ClassNameFilter exceptionClassFilter;

    /**
     * Creates an object.
     *
     * @param name                 pointcut name
     * @param methodFilter         intercepted method filter. Can be null
     * @param interceptor          interceptor
     * @param exceptionClassFilter exception class filter. Can be null
     * @param singleton            if true pointcut generates single join point
     */
    public CatchPointcut(String name, QualifiedMethodFilter methodFilter, InterceptorConfiguration interceptor,
                         ClassNameFilter exceptionClassFilter, boolean singleton) {
        super(name, methodFilter, interceptor, singleton, 0);

        this.exceptionClassFilter = exceptionClassFilter;
    }

    public ClassNameFilter getExceptionClassFilter() {
        return exceptionClassFilter;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new CatchInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CatchPointcut))
            return false;

        CatchPointcut pointcut = (CatchPointcut) o;
        return super.equals(o) && Objects.equals(exceptionClassFilter, pointcut.exceptionClassFilter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(exceptionClassFilter);
    }

    @Override
    public String toString() {
        return super.toString() + messages.toString(exceptionClassFilter).toString();
    }

    private interface IMessages {
        @DefaultMessage(", exception class filter: [{0}]")
        ILocalizedMessage toString(ClassNameFilter exceptionClassFilter);
    }
}
