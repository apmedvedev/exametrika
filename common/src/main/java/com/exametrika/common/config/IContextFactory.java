/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;


/**
 * The {@link IContextFactory} represents a factory of specific configuration load context.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IContextFactory {
    /**
     * Creates load context of specific configuration.
     *
     * @return load context
     */
    IConfigurationFactory createContext();
}
