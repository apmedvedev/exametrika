/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.config;

import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link DynamicInterceptorConfiguration} is an dynamic interceptor configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DynamicInterceptorConfiguration extends InterceptorConfiguration {
    public abstract IDynamicInterceptor createInterceptor();
}
