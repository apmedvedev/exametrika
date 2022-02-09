/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.values.IMetricValue;


/**
 * The {@link IMeter} represents a meter - a measuring instrument.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IMeter {
    /**
     * Returns metric type.
     *
     * @return metric type
     */
    String getMetricType();

    /**
     * Does meter have provider?
     *
     * @return true if meter has provider
     */
    boolean hasProvider();

    /**
     * Performs measurement using specified measurement povider.
     */
    void measure();

    /**
     * Performs measurement using specified measurement value.
     *
     * @param value
     */
    void measure(Object value);

    /**
     * Extracts measurement values.
     *
     * @param approximationMultiplier approximation multiplier
     * @param force                   if true extracts unconditionally
     * @param clear                   if true collected measurement results are cleared
     * @return metric value or null if meter does not have measurement
     */
    IMetricValue extract(double approximationMultiplier, boolean force, boolean clear);
}
