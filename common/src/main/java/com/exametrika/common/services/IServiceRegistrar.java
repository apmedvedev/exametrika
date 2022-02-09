/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;


/**
 * The {@link IServiceRegistrar} represents a service registrar.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IServiceRegistrar {
    /**
     * Registers service.
     *
     * @param name    service name
     * @param service service
     */
    void register(String name, IService service);
}
