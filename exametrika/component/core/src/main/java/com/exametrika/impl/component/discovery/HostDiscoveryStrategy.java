/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import com.exametrika.api.component.config.model.HostDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link HostDiscoveryStrategy} is a host component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostDiscoveryStrategy extends BaseComponentDiscoveryStrategy {
    public HostDiscoveryStrategy(HostDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(configuration, context);
    }
}
