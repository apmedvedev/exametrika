/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;


/**
 * The {@link IHistogramValue} represents a histogram field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IHistogramValue extends IFieldValue {
    /**
     * Returns number of histogram bins.
     *
     * @return number of histogram bins
     */
    int getBinCount();

    /**
     * Returns bin value by index.
     *
     * @param index bin index
     * @return bin value
     */
    long getBin(int index);

    /**
     * Returns value of minimal-out-of-bounds bin
     *
     * @return value of minimal-out-of-bounds bin
     */
    long getMinOutOfBounds();

    /**
     * Returns value of maximal-out-of-bounds bin
     *
     * @return value of maximal-out-of-bounds bin
     */
    long getMaxOutOfBounds();
}
