/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IAggregationFilter;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AggregationFilterSchemaConfiguration} represents a configuration of schema of aggregation filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AggregationFilterSchemaConfiguration extends Configuration {
    public abstract IAggregationFilter createFilter(IDatabaseContext context);
}
