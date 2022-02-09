/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IMeasurementExpressionContext} represents a measurement expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementExpressionContext extends IExpressionContext {
    /**
     * Does measurement have metric with specified name?
     *
     * @param name metric name
     * @return true if measurement has metric with specified name
     */
    boolean hasMetric(String name);

    /**
     * Returns metric value.
     *
     * @param name metric name
     * @return metric value or null if metric is not found
     */
    Object metric(String name);
}
