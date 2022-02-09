/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;


/**
 * The {@link CheckPermissionStrategySchemaConfiguration} represents a configuration of schema of check permission strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class CheckPermissionStrategySchemaConfiguration extends Configuration {
    public abstract ICheckPermissionStrategy createStrategy(IDatabaseContext context);
}
