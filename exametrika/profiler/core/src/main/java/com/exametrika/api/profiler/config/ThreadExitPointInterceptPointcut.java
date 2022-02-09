/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.profiler.probes.ThreadInterceptInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link ThreadExitPointInterceptPointcut} represents an thread intercept pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ThreadExitPointInterceptPointcut extends InterceptPointcut {
    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param kinds        intercept kinds
     * @param interceptor  interceptor
     * @param useParams    use params
     * @param priority     priority
     */
    public ThreadExitPointInterceptPointcut(String name, QualifiedMethodFilter methodFilter, Set<Kind> kinds, InterceptorConfiguration interceptor,
                                            boolean useParams, int priority) {
        super(name, methodFilter, kinds, interceptor, useParams, false, priority);
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new ThreadInterceptInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ThreadExitPointInterceptPointcut))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
