/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.api.aggregator.common.model.IScopeName;


/**
 * The {@link IScope} represents a measurement scope.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IScope {
    /**
     * Returns scope name.
     *
     * @return scope name
     */
    IScopeName getName();

    /**
     * Is scope permanent or initiated by transaction?
     *
     * @return if true scope is permanent
     */
    boolean isPermanent();

    /**
     * Returns component type of entry point if scope is not permanent.
     *
     * @return component type of entry point if scope is not permanent
     */
    String getEntryPointComponentType();

    /**
     * Activates scope in current thread.
     */
    void begin();

    /**
     * Deactivates scope in current thread.
     */
    void end();
}
