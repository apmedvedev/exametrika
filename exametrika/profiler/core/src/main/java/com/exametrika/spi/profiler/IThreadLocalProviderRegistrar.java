/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IThreadLocalProviderRegistrar} represents a registrar of thread local providers.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadLocalProviderRegistrar {
    /**
     * Registers thread local providers.
     *
     * @param registry registry of thread local providers
     */
    void register(IThreadLocalProviderRegistry registry);
}
