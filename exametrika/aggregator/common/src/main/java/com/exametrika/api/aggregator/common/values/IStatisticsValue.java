/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;


/**
 * The {@link IStatisticsValue} represents a statistics field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IStatisticsValue extends IFieldValue {
    /**
     * Returns sum of squares.
     *
     * @return sum of squares
     */
    double getSumSquares();
}
