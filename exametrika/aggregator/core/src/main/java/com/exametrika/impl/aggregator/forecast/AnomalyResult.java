/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link AnomalyResult} is a result of anomaly score computation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyResult {
    private final float anomalyScore;
    private final int behaviorType;
    private boolean anomaly;
    private boolean primaryAnomaly;

    public AnomalyResult(float anomalyScore, int behaviorType, boolean anomaly, boolean primaryAnomaly) {
        this.anomalyScore = anomalyScore;
        this.behaviorType = behaviorType;
        this.anomaly = anomaly;
        this.primaryAnomaly = primaryAnomaly;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public boolean isPrimaryAnomaly() {
        return primaryAnomaly;
    }

    public float getAnomalyScore() {
        return anomalyScore;
    }

    public int getBehaviorType() {
        return behaviorType;
    }

    @Override
    public String toString() {
        return Float.toString(anomalyScore);
    }
}
