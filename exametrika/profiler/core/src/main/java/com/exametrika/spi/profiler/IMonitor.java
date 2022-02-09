/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link IMonitor} represents a monitor.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMonitor extends ILifecycle {
    /**
     * Performs monitor measurement.
     *
     * @param time         measurement time
     * @param period       measurement period since previous measurement
     * @param measurements list to add extracted measurements
     * @param force        if true performs measurement unconditionally
     */
    void measure(List<Measurement> measurements, long time, long period, boolean force);
}
