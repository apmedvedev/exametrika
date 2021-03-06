/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;


/**
 * The {@link IConfigurationFactory} represents a configuration factory.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IConfigurationFactory {
    /**
     * Creates configuration.
     *
     * @param context load context
     * @return configuration or null if configuration is not present
     */
    Object createConfiguration(ILoadContext context);
}
