/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.host;


/**
 * The {@link IProcessNamingStrategy} represents a naming strategy for scopes of host processes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProcessNamingStrategy {
    /**
     * Returns process name. Process name must not contain period characters.
     *
     * @param context process context
     * @return process name
     */
    String getName(IProcessContext context);
}
