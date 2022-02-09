/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AggregationLogTransformerSchemaConfiguration} represents a configuration of schema of aggregation log transformer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AggregationLogTransformerSchemaConfiguration extends Configuration {
    public abstract IAggregationLogTransformer createTransformer(IDatabaseContext context);
}
