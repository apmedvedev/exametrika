/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;


/**
 * The {@link AlternativeMeanAbsolutePercentageErrorMetric} is a alternative mean absolute percentage error metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AlternativeMeanAbsolutePercentageErrorMetric {
    private double errorSum;
    private double sum;

    public double getMetric() {
        return errorSum / sum;
    }

    public double compute(double value, double prediction) {
        errorSum += Math.abs(value - prediction);
        sum += Math.abs(value);

        return getMetric();
    }

    public void clear() {
        errorSum = 0;
        sum = 0;
    }
}
