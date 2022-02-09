/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.IInterceptor;


/**
 * The {@link IStackInterceptor} represents a static stack interceptor interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IStackInterceptor extends IInterceptor {
    Object onEnter(int index, int version);

    void onReturn(Object param);
}
