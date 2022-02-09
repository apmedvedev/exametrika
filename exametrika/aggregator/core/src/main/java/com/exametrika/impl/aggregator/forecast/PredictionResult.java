/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link PredictionResult} is a result of prediction computation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PredictionResult {
    private final float value;
    private final int behaviorType;

    public PredictionResult(float value, int behaviorType) {
        this.value = value;
        this.behaviorType = behaviorType;
    }

    public float getValue() {
        return value;
    }

    public int getBehaviorType() {
        return behaviorType;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
}
