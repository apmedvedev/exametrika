/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AggregationLogFilterSchemaConfiguration} represents a configuration of schema of aggregation log filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AggregationLogFilterSchemaConfiguration extends Configuration {
    public abstract IAggregationLogFilter createFilter(IDatabaseContext context);
}
