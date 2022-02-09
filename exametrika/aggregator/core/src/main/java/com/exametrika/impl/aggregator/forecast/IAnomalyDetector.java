/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link IAnomalyDetector} represents an anomaly detector.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAnomalyDetector {
    /**
     * Returns identifier of anomaly detector.
     *
     * @return identifier of anomaly detector
     */
    int getId();

    /**
     * Computes anomaly.
     *
     * @param time  time
     * @param value value
     * @return anomaly result
     */
    AnomalyResult computeAnomaly(long time, float value);
}
