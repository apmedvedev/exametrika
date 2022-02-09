/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.spi.profiler.boot.ThreadLocalContainer;


/**
 * The {@link IThreadLocalAccessor} represents an acessor to thread local data.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadLocalAccessor {
    /**
     * Returns thread local data container.
     *
     * @return thread local data container
     */
    ThreadLocalContainer getContainer();
}
