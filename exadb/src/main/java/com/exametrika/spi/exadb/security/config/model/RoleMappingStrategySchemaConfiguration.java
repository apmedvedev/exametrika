/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.IRoleMappingStrategy;


/**
 * The {@link RoleMappingStrategySchemaConfiguration} represents a configuration of schema of role mapping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class RoleMappingStrategySchemaConfiguration extends Configuration {
    public abstract IRoleMappingStrategy createStrategy(IDatabaseContext context);
}
