/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link ComponentDiscoveryStrategySchemaConfiguration} represents a configuration of schema of component discovery strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentDiscoveryStrategySchemaConfiguration extends Configuration {
    public abstract IComponentDiscoveryStrategy createStrategy(IDatabaseContext context);
}
