/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.config;

import com.exametrika.api.instrument.config.Pointcut;


/**
 * The {@link IInstrumentationLoadContext} represents a instrumentation configuration load context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInstrumentationLoadContext {
    /**
     * Adds pointcut.
     *
     * @param pointcut pointcut
     */
    void addPointcut(Pointcut pointcut);

    /**
     * Sets maximum number of join points.
     *
     * @param value maximum number of join points
     */
    void setMaxJoinPointCount(int value);
}
