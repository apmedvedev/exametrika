/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link IGauge} represents a gauge.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IGauge extends IFieldMeter {
    /**
     * Performs measurement using specified value. Value must contain absolute measurement value.
     *
     * @param value absolute measurement value
     */
    void measure(long value);
}
