/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;


/**
 * The {@link IServiceProvider} represents a provider of service.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IServiceProvider {
    /**
     * Registers services of this service provider.
     *
     * @param registrar service registrar
     */
    void register(IServiceRegistrar registrar);
}
