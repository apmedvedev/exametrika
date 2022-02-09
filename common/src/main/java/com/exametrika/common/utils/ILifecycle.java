/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link ILifecycle} represents a component that has lifecycle.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ILifecycle {
    /**
     * Starts a component.
     *
     * @throws InvalidStateException if already started
     */
    void start();

    /**
     * Stops a component.
     */
    void stop();
}
