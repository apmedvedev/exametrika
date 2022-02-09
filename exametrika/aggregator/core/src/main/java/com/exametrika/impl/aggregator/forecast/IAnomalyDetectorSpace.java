/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link IAnomalyDetectorSpace} represents an anomaly detector space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAnomalyDetectorSpace {
    /**
     * Creates anomaly detector.
     *
     * @param parameters parameters
     * @return anomaly detector
     */
    IAnomalyDetector createAnomalyDetector(AnomalyDetector.Parameters parameters);

    /**
     * Opens anomaly detector.
     *
     * @param id         identifier
     * @param parameters parameters
     * @return anomaly detector
     */
    IAnomalyDetector openAnomalyDetector(int id, AnomalyDetector.Parameters parameters);
}
