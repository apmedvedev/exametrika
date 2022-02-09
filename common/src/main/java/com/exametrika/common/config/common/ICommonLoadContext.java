/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.common;


/**
 * The {@link ICommonLoadContext} represents a common configuration load context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICommonLoadContext {
    /**
     * Returns runtime mode.
     *
     * @return runtime mode
     */
    RuntimeMode getRuntimeMode();
}
