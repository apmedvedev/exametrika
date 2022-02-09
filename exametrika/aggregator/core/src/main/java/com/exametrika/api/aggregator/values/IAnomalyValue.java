/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;


/**
 * The {@link IAnomalyValue} represents a anomaly field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IAnomalyValue extends IFieldValue {
    /**
     * Returns anomaly score.
     *
     * @return anomaly score
     */
    float getAnomalyScore();

    /**
     * Returns identifier of behavior type.
     *
     * @return identifier of behavior type
     */
    int getBehaviorType();

    /**
     * Does current value represents anomaly?
     *
     * @return true if current value is anomalous
     */
    boolean isAnomaly();

    /**
     * Is current value primary or secondary (already seen) anomaly?
     *
     * @return true if current value is primary anomaly
     */
    boolean isPrimaryAnomaly();

    /**
     * Returns page index of forecaster/anomaly detector for this value.
     *
     * @return page index of forecaster/anomaly detector for this value
     */
    int getId();
}
