/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IThreadLocalProviderRegistry} represents a registry of thread local providers.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadLocalProviderRegistry {
    /**
     * Adds thread local provider.
     *
     * @param provider thread local provider
     */
    void addProvider(IThreadLocalProvider provider);
}
