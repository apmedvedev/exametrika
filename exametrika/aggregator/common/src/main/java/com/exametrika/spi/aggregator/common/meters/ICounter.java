/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link ICounter} represents a counter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ICounter extends IFieldMeter {
    /**
     * Begins measurement using specified value. Value must contain absolute measurement value.
     *
     * @param value absolute measurement value
     */
    void beginMeasure(long value);

    /**
     * Ends measurement using specified value. Value must contain absolute measurement value.
     *
     * @param value absolute measurement value
     */
    void endMeasure(long value);

    /**
     * Ends previous measurement and begins next measurement using specified value. Value must contain absolute measurement value.
     *
     * @param value absolute measurement value
     */
    void measure(long value);

    /**
     * Performs measurement using specified value. Value must contain measurement delta value.
     *
     * @param value measurement delta value
     */
    void measureDelta(long value);
}
