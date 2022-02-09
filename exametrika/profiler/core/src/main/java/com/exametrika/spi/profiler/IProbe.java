/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link IProbe} represents a probe.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProbe extends ILifecycle {
    /**
     * Is probe a system probe (used to measure agent itself)?
     *
     * @return returns true if probe is system probe
     */
    boolean isSystem();

    /**
     * Is probe stack-based probe?
     *
     * @return returns true if probe is stack-based probe
     */
    boolean isStack();

    /**
     * Is probe calibrated?
     *
     * @return true if probe is calibrated
     */
    boolean isCalibrated();

    /**
     * Performs initial probe calibration.
     *
     * @param force if true performs calibration unconditionally
     */
    void calibrate(boolean force);

    /**
     * Enables or disables probe.
     *
     * @param value true if probe is enabled
     */
    void setEnabled(boolean value);

    /**
     * Is given class a probe interceptor?
     *
     * @param className name of checked class
     * @return true if class with specified name is a probe interceptor
     */
    boolean isProbeInterceptor(String className);

    /**
     * Creates probe collector in current thread for specified scope.
     *
     * @param scope scope
     * @return probe collector or null if probe does not support creation of probe collectors
     */
    IProbeCollector createCollector(IScope scope);

    /**
     * Called periodically on timer.
     */
    void onTimer();
}
