/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.config;


import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link IProfilerLoadContext} represents a profiler configuration load context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProfilerLoadContext {
    /**
     * Adds monitor.
     *
     * @param monitor monitor
     */
    void addMonitor(MonitorConfiguration monitor);

    /**
     * Adds probe.
     *
     * @param probe probe
     */
    void addProbe(ProbeConfiguration probe);

    /**
     * Adds permanent scope.
     *
     * @param scope permanent scope
     */
    void addPermanentScope(ScopeConfiguration scope);

    /**
     * Adds measurement strategy.
     *
     * @param measurementStrategy measurement strategy
     */
    void addMeasurementStrategy(MeasurementStrategyConfiguration measurementStrategy);
}
