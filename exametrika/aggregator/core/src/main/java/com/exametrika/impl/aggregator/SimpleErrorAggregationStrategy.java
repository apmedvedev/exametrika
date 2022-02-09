/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.common.utils.NameFilter;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;


/**
 * The {@link SimpleErrorAggregationStrategy} represents a configuration of simple error aggregation strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleErrorAggregationStrategy implements IErrorAggregationStrategy {
    private final NameFilter filter;
    private String prefix;

    public SimpleErrorAggregationStrategy(String pattern, String prefix) {
        if (pattern != null)
            filter = new NameFilter(pattern);
        else
            filter = null;
        this.prefix = prefix;
    }

    @Override
    public String getDerivedType(String errorType) {
        if (filter != null && !filter.match(errorType))
            return null;

        if (prefix != null)
            return prefix + errorType;
        else
            return errorType;
    }
}