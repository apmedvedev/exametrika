/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;


/**
 * The {@link IStandardValue} represents a standard field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IStandardValue extends IFieldValue {
    /**
     * Returns count.
     *
     * @return count
     */
    long getCount();

    /**
     * Returns min.
     *
     * @return min
     */
    long getMin();

    /**
     * Returns max.
     *
     * @return max
     */
    long getMax();

    /**
     * Returns sum.
     *
     * @return sum
     */
    long getSum();
}
