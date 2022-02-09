/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.config;

import com.exametrika.common.utils.Assert;


/**
 * The {@link StaticInterceptorConfiguration} is a static interceptor configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StaticInterceptorConfiguration extends InterceptorConfiguration {
    private final Class<?> interceptorClass;

    public StaticInterceptorConfiguration(Class<?> interceptorClass) {
        Assert.notNull(interceptorClass);

        this.interceptorClass = interceptorClass;
    }

    public Class getInterceptorClass() {
        return interceptorClass;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StaticInterceptorConfiguration))
            return false;

        StaticInterceptorConfiguration configuration = (StaticInterceptorConfiguration) o;
        return interceptorClass == configuration.interceptorClass;
    }

    @Override
    public int hashCode() {
        return interceptorClass.hashCode();
    }
}
