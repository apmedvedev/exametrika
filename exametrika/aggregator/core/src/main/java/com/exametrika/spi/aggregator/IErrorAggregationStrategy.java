/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IErrorAggregationStrategy} represents an aggregation strategy for error types.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IErrorAggregationStrategy {
    /**
     * Returns derived error type for specified error type.
     *
     * @param errorType
     * @return derived error type or null if error should be ignored
     */
    String getDerivedType(String errorType);
}
