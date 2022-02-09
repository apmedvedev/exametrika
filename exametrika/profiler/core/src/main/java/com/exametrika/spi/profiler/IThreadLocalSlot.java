/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IThreadLocalSlot} represents a slot of thread local data.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadLocalSlot {
    /**
     * Returns thread local data associated with slot.
     *
     * @return thread local data associated with slot
     */
    <T> T get();
}
