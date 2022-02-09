/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.discovery;

import com.exametrika.api.metrics.exa.server.config.model.ExaServerDiscoveryStrategySchemaConfiguration;
import com.exametrika.impl.component.discovery.BaseComponentDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExaServerDiscoveryStrategy} is a exa server component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerDiscoveryStrategy extends BaseComponentDiscoveryStrategy {
    public ExaServerDiscoveryStrategy(ExaServerDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(configuration, context);
    }
}
