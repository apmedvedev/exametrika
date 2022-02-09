/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link ITimerListener} is a listener of timer events.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITimerListener {
    /**
     * Called when timer is elapsed.
     */
    void onTimer();
}
