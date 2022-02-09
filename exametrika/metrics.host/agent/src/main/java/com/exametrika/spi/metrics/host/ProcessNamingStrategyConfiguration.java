/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.host;

import com.exametrika.common.config.Configuration;


/**
 * The {@link ProcessNamingStrategyConfiguration} is a configuration for process naming strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ProcessNamingStrategyConfiguration extends Configuration {
    public abstract IProcessNamingStrategy createStrategy();
}
