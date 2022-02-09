/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestGroupingStrategy;


/**
 * The {@link RequestGroupingStrategyConfiguration} is a request grouping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class RequestGroupingStrategyConfiguration extends Configuration {
    public abstract IRequestGroupingStrategy createStrategy(IProbeContext context);
}
