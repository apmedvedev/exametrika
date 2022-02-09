/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link GroupDiscoveryStrategySchemaConfiguration} represents a configuration of schema of group discovery strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class GroupDiscoveryStrategySchemaConfiguration extends Configuration {
    public abstract IGroupDiscoveryStrategy createStrategy(IDatabaseContext context);
}
