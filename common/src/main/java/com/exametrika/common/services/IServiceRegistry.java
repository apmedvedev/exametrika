/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;


/**
 * The {@link IServiceRegistry} represents a service registry.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IServiceRegistry {
    /**
     * Finds initialization parameter.
     *
     * @param name - parameter name
     * @return initialization parameter or null if parameter with specified name is not found
     */
    <T> T findParameter(String name);

    /**
     * Finds service by name.
     *
     * @param name service name
     * @return service or null if service is not found
     */
    <T extends IService> T findService(String name);

    /**
     * Updates configuration of services.
     *
     * @param path path to new configuration
     */
    void setConfiguration(String path);
}
