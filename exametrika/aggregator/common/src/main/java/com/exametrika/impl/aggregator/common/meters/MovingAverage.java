/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The {@link MovingAverage} contains methods to compute moving average.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MovingAverage {
    private final int windowSize;
    private final Deque<Double> slidingWindow = new ArrayDeque<Double>();
    private double total;

    public MovingAverage(int windowSize) {
        this.windowSize = windowSize;
    }

    public double getTotal() {
        return total;
    }

    public double next(double newVal) {
        if (slidingWindow.size() == windowSize)
            total -= slidingWindow.removeLast();

        slidingWindow.addFirst(newVal);
        total += newVal;
        return total / slidingWindow.size();
    }

    public void clear() {
        slidingWindow.clear();
        total = 0;
    }
}
