/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.metrics.jvm.probes.HttpInterceptInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link HttpInterceptPointcut} represents an HTTP intercept pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HttpInterceptPointcut extends InterceptPointcut {
    private final String interceptorMethodSignature;

    public HttpInterceptPointcut(String name, QualifiedMethodFilter methodFilter, Set<Kind> kinds, InterceptorConfiguration interceptor,
                                 String interceptorMethodSignature, int priority) {
        super(name, methodFilter, kinds, interceptor, false, false, priority);

        Assert.notNull(interceptorMethodSignature);

        this.interceptorMethodSignature = interceptorMethodSignature;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new HttpInterceptInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug, interceptorMethodSignature);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpInterceptPointcut))
            return false;

        HttpInterceptPointcut pointcut = (HttpInterceptPointcut) o;
        return super.equals(o) && interceptorMethodSignature.equals(pointcut.interceptorMethodSignature);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + interceptorMethodSignature.hashCode();
    }
}
