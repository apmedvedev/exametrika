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
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MonitorInterceptInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link MonitorInterceptPointcut} represents an monitor intercept pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonitorInterceptPointcut extends Pointcut {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<Kind> kinds;

    /**
     * Invocation kind.
     */
    public enum Kind {
        /**
         * Before monitor enter.
         */
        BEFORE_ENTER,

        /**
         * After monitor enter.
         */
        AFTER_ENTER,

        /**
         * Before monitor exit.
         */
        BEFORE_EXIT,

        /**
         * After monitor exit.
         */
        AFTER_EXIT
    }

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param kinds        intercept kinds
     * @param interceptor  interceptor
     * @param singleton    if true pointcut generates single join point
     */
    public MonitorInterceptPointcut(String name, QualifiedMethodFilter methodFilter, Set<Kind> kinds,
                                    InterceptorConfiguration interceptor, boolean singleton) {
        super(name, methodFilter, interceptor, singleton, 0);

        Assert.notNull(kinds);

        this.kinds = kinds;
    }

    public Set<Kind> getKinds() {
        return kinds;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new MonitorInterceptInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MonitorInterceptPointcut))
            return false;

        MonitorInterceptPointcut pointcut = (MonitorInterceptPointcut) o;
        return super.equals(o) && Objects.equals(kinds, pointcut.kinds);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(kinds);
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
