/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;


/**
 * The {@link MeanAbsoluteScaledErrorMetric} is a mean absolute scaled error metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeanAbsoluteScaledErrorMetric {
    private double error;
    private double sum;
    private double prevValue = Double.NaN;

    public double getMetric() {
        return error;
    }

    public double compute(double value, double prediction) {
        if (!Double.isNaN(prevValue)) {
            sum += Math.abs(value - prevValue);
            error += Math.abs(value - prediction) / sum;
        }

        prevValue = value;

        return error;
    }

    public void clear() {
        sum = 0;
        error = 0;
    }
}
