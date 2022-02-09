/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link IStandardFieldCollector} represents a standard field collector.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStandardFieldCollector extends IFieldCollector {
    /**
     * Updates field values with specified value and count.
     *
     * @param count count to update
     * @param value value to update
     */
    void update(long count, long value);
}
