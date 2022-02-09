/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestMappingStrategy;


/**
 * The {@link RequestMappingStrategyConfiguration} is a request mapping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class RequestMappingStrategyConfiguration extends Configuration {
    public abstract IRequestMappingStrategy createStrategy(IProbeContext context);
}
