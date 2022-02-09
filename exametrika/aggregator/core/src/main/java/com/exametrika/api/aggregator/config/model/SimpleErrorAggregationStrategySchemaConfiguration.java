/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.SimpleErrorAggregationStrategy;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;


/**
 * The {@link SimpleErrorAggregationStrategySchemaConfiguration} represents a configuration of simple error aggregation strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleErrorAggregationStrategySchemaConfiguration extends ErrorAggregationStrategySchemaConfiguration {
    private String pattern;
    private String prefix;

    public SimpleErrorAggregationStrategySchemaConfiguration(String pattern, String prefix) {
        this.pattern = pattern;
        this.prefix = prefix;
    }

    @Override
    public IErrorAggregationStrategy createStrategy() {
        return new SimpleErrorAggregationStrategy(pattern, prefix);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleErrorAggregationStrategySchemaConfiguration))
            return false;

        SimpleErrorAggregationStrategySchemaConfiguration configuration = (SimpleErrorAggregationStrategySchemaConfiguration) o;
        return Objects.equals(pattern, configuration.pattern) && Objects.equals(prefix, configuration.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pattern, prefix);
    }
}
