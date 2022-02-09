/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link Pointcut} represents a pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Pointcut extends Configuration {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final QualifiedMethodFilter methodFilter;
    private final InterceptorConfiguration interceptor;
    private final boolean singleton;
    private final int priority;

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param interceptor  interceptor
     * @param singleton    if true pointcut generates single join point
     * @param priority     relative priority of pointcut applications. Pointcuts with higher priority will be applied first.
     */
    public Pointcut(String name, QualifiedMethodFilter methodFilter, InterceptorConfiguration interceptor, boolean singleton,
                    int priority) {
        Assert.notNull(name);
        Assert.notNull(interceptor);

        this.name = name;
        this.methodFilter = methodFilter;
        this.interceptor = interceptor;
        this.singleton = singleton;
        this.priority = priority;
    }

    public final String getName() {
        return name;
    }

    public final QualifiedMethodFilter getMethodFilter() {
        return methodFilter;
    }

    public final InterceptorConfiguration getInterceptor() {
        return interceptor;
    }

    public final boolean isSingleton() {
        return singleton;
    }

    public final int getPriority() {
        return priority;
    }

    public abstract IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                                     String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                                     IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Pointcut))
            return false;

        Pointcut pointcut = (Pointcut) o;
        return name.equals(pointcut.name) && Objects.equals(methodFilter, pointcut.methodFilter) &&
                interceptor.equals(pointcut.interceptor) && singleton == pointcut.singleton && priority == pointcut.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, methodFilter, interceptor, singleton, priority);
    }

    @Override
    public String toString() {
        return messages.toString(name, getClass().getSimpleName(), methodFilter, interceptor).toString();
    }

    private interface IMessages {
        @DefaultMessage("name: {0}, type: {1}, method filter: [{2}], {3}")
        ILocalizedMessage toString(String name, String type, QualifiedMethodFilter methodFilter, InterceptorConfiguration interceptor);
    }
}
