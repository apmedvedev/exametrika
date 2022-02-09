/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.common.meters.ILogFilter;


/**
 * The {@link LogFilterConfiguration} is a configuration of log filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class LogFilterConfiguration extends Configuration {
    public abstract ILogFilter createFilter();
}
