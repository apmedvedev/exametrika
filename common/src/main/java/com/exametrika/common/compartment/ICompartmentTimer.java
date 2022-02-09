/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;


/**
 * The {@link ICompartmentTimer} is a compartment timer based on ticks from {@link ICompartmentTimerProcessor}.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompartmentTimer extends ICompartmentTimerProcessor {
    /**
     * Returns timer period.
     * @return timer period in milliseconds
     */
    long getPeriod();

    /**
     * Sets timer period.
     * @param period new timer period in milliseconds
     */
    void setPeriod(long period);

    /**
     * Is timer started?
     *
     * @return true if timer is started
     */
    boolean isStarted();

    /**
     * Returns number of timer firings since last restart.
     *
     * @return number of timer firings since last restart
     */
    int getAttempt();

    /**
     * Starts timer (if not started).
     */
    void start();

    /**
     * Restarts timer from current time.
     */
    void restart();

    /**
     * Fires timer immediately.
     */
    void fire();

    /**
     * Postpones firing on nearest timer tick. Starts timer if not started.
     */
    void delayedFire();

    /**
     * Stops timer.
     */
    void stop();
}
