/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.core.ICacheCategorizationStrategy;

/**
 * The {@link CacheCategorizationStrategyConfiguration} represents a configuration of cache categorization strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class CacheCategorizationStrategyConfiguration extends Configuration {
    public abstract ICacheCategorizationStrategy createStrategy();
}
