/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.profiler.probes.StackInterceptInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link StackInterceptPointcut} represents a stack intercept pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackInterceptPointcut extends InterceptPointcut {
    /**
     * Creates an object.
     *
     * @param name             pointcut name
     * @param methodFilter     intercepted method filter. Can be null
     * @param interceptorClass interceptor class
     */
    public StackInterceptPointcut(String name, QualifiedMethodFilter methodFilter, Class<?> interceptorClass) {
        super(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                new StaticInterceptorConfiguration(interceptorClass), false, false, 0);
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new StackInterceptInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackInterceptPointcut))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
