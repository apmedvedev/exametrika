/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.util.Set;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link InterceptPointcut} represents an intercept pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class InterceptPointcut extends Pointcut {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<Kind> kinds;
    private final boolean useParams;

    /**
     * Invocation kind.
     */
    public enum Kind {
        /**
         * Method enter.
         */
        ENTER,

        /**
         * Method exit by returning from method.
         */
        RETURN_EXIT,

        /**
         * Method exit by throwing exception.
         */
        THROW_EXIT
    }

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param kinds        intercept kinds
     * @param interceptor  interceptor
     * @param useParams    use params
     * @param singleton    if true pointcut generates single join point
     * @param priority     pointcut priority
     */
    public InterceptPointcut(String name, QualifiedMethodFilter methodFilter, Set<Kind> kinds, InterceptorConfiguration interceptor,
                             boolean useParams, boolean singleton, int priority) {
        super(name, methodFilter, interceptor, singleton, priority);

        Assert.notNull(kinds);

        this.kinds = kinds;
        this.useParams = useParams;
    }

    public Set<Kind> getKinds() {
        return kinds;
    }

    public boolean getUseParams() {
        return useParams;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new InterceptInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InterceptPointcut))
            return false;

        InterceptPointcut pointcut = (InterceptPointcut) o;
        return super.equals(o) && Objects.equals(kinds, pointcut.kinds) && Objects.equals(useParams, pointcut.useParams);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(kinds, useParams);
    }

    @Override
    public String toString() {
        return super.toString() + messages.toString(kinds).toString();
    }

    private interface IMessages {
        @DefaultMessage(", kinds: {0}")
        ILocalizedMessage toString(Set<Kind> kind);
    }
}
