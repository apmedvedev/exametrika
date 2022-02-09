/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;


/**
 * The {@link IThreadExitPointInterceptor} represents a static thread exit point interceptor interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadExitPointInterceptor extends IInterceptor {
    /**
     * Called on task execute interception.
     *
     * @param index   join point index
     * @param version join point version
     * @param task    intercepted runnable task
     * @return wrapped runnable task
     */
    Runnable onExecute(int index, int version, Object task);
}
