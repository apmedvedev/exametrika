/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IAggregationAnalyzer;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AggregationAnalyzerSchemaConfiguration} represents a configuration of schema of aggregation analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AggregationAnalyzerSchemaConfiguration extends Configuration {
    public abstract IAggregationAnalyzer createAnalyzer(IDatabaseContext context);
}
