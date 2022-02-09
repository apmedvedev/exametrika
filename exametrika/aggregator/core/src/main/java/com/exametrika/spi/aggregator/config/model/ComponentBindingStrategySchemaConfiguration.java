/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link ComponentBindingStrategySchemaConfiguration} represents a configuration of schema of component binding strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentBindingStrategySchemaConfiguration extends Configuration {
    public abstract IComponentBindingStrategy createStrategy(IDatabaseContext context);
}
