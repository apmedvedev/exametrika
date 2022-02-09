/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link ComponentDeletionStrategySchemaConfiguration} represents a configuration of schema of component deletion strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentDeletionStrategySchemaConfiguration extends Configuration {
    public abstract IComponentDeletionStrategy createStrategy(IDatabaseContext context);
}
