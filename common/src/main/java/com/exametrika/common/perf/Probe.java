/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.perf;


/**
 * The {@link Probe} is an abstract implementation of {@link IProbe}.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @see IProbe
 * @see Benchmark
 */
public abstract class Probe implements IProbe {
    @Override
    public void beforeMeasure() {
    }

    @Override
    public void afterWarmUp() {
    }

    @Override
    public void afterMeasure(long count, long time, long totalTime) {
    }

    @Override
    public long run() {
        runOnce();
        return 1;
    }

    public void runOnce() {
    }
}
