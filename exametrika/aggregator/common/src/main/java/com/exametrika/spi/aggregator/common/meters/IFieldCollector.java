/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.values.IFieldValue;


/**
 * The {@link IFieldCollector} represents a field collector.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFieldCollector {
    /**
     * Updates field values with specified value.
     *
     * @param value value to update
     */
    void update(long value);

    /**
     * Extracts measurements.
     *
     * @param count                   measurements count
     * @param approximationMultiplier approximation multiplier
     * @param clear                   if true collected measurement results are cleared
     * @return measurements
     */
    IFieldValue extract(long count, double approximationMultiplier, boolean clear);
}
