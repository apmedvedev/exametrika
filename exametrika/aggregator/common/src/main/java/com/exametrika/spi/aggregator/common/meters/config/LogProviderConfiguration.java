/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;


/**
 * The {@link LogProviderConfiguration} is a configuration of log provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class LogProviderConfiguration extends Configuration {
    public abstract ILogProvider createProvider();
}
