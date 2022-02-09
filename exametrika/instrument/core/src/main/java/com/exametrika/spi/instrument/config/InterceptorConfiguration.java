/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.config;

import com.exametrika.common.config.Configuration;


/**
 * The {@link InterceptorConfiguration} is an abstract interceptor configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class InterceptorConfiguration extends Configuration {
    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
