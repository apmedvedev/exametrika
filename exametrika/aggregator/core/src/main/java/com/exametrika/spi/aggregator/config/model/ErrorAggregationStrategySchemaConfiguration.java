/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;

/**
 * The {@link ErrorAggregationStrategySchemaConfiguration} represents a configuration of schema of error aggregation strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ErrorAggregationStrategySchemaConfiguration extends Configuration {
    public abstract IErrorAggregationStrategy createStrategy();
}
