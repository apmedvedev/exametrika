/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;


/**
 * The {@link RootMeanSquareErrorMetric} is a root mean square error metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class RootMeanSquareErrorMetric {
    private double sumSquares;
    private double sum;
    private int count;

    public double getMetric() {
        return Math.sqrt(sumSquares / count);
    }

    public double getVariationCoefficient() {
        double mean = sum / count;
        return getMetric() / mean;
    }

    public double compute(double value, double prediction) {
        sum += Math.abs(value);
        sumSquares += (value - prediction) * (value - prediction);
        count++;

        return getMetric();
    }

    public void clear() {
        sum = 0;
        sumSquares = 0;
        count = 0;
    }
}
