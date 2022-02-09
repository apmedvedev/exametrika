/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.ArrayGetInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link ArrayGetPointcut} represents a array get pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ArrayGetPointcut extends Pointcut {
    private final boolean useParams;

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param interceptor  interceptor
     * @param useParams    use params
     * @param singleton    if true pointcut generates single join point
     */
    public ArrayGetPointcut(String name, QualifiedMethodFilter methodFilter,
                            InterceptorConfiguration interceptor, boolean useParams, boolean singleton) {
        super(name, methodFilter, interceptor, singleton, 0);

        this.useParams = useParams;
    }

    public boolean getUseParams() {
        return useParams;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new ArrayGetInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ArrayGetPointcut))
            return false;

        ArrayGetPointcut pointcut = (ArrayGetPointcut) o;
        return super.equals(o) && Objects.equals(useParams, pointcut.useParams);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(useParams);
    }
}
