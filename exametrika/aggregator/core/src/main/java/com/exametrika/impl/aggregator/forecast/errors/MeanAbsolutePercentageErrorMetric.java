/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;


/**
 * The {@link MeanAbsolutePercentageErrorMetric} is a mean absolute percentage error metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeanAbsolutePercentageErrorMetric {
    private double errorSum;
    private int count;

    public double getMetric() {
        return errorSum / count;
    }

    public double compute(double value, double prediction) {
        errorSum += Math.abs((value - prediction) / value);
        count++;

        return getMetric();
    }

    public void clear() {
        errorSum = 0;
        count = 0;
    }
}
