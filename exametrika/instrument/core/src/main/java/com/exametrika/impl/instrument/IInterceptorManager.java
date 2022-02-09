/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import com.exametrika.spi.instrument.IInterceptorAllocator;


/**
 * The {@link IInterceptorManager} represents an interceptor manager.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInterceptorManager extends IInterceptorAllocator {
    /**
     * Returns count of allocated join points.
     *
     * @return count of allocated join points
     */
    int getJoinPointCount();

    /**
     * Stops all interceptors of specified class and frees interceptors resources.
     *
     * @param classLoader class loader. Can be null if class is loaded by bootstrap class loader
     * @param className   class name
     */
    void free(ClassLoader classLoader, String className);

    /**
     * Stops all interceptors and frees their resources.
     */
    void freeAll();

    /**
     * Stops all interceptors which classes are unloaded.
     */
    void freeUnloaded();
}
