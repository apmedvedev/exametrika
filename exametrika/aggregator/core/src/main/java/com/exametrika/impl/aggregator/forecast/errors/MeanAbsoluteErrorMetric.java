/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;


/**
 * The {@link MeanAbsoluteErrorMetric} is a mean absolute error metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeanAbsoluteErrorMetric {
    private double errorSum;
    private int count;

    public double getMetric() {
        return errorSum / count;
    }

    public double compute(double value, double prediction) {
        errorSum += Math.abs(value - prediction);
        count++;

        return getMetric();
    }

    public void clear() {
        errorSum = 0;
        count = 0;
    }
}
