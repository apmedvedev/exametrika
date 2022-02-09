/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IThreadLocalProvider} represents a provider of thread local data.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadLocalProvider {
    /**
     * Sets thread local slot for this provider.
     *
     * @param slot thread local slot for this provider
     */
    void setSlot(IThreadLocalSlot slot);

    /**
     * Allocates thread local data of this provider.
     *
     * @return thread local data of this provider
     */
    Object allocate();
}
