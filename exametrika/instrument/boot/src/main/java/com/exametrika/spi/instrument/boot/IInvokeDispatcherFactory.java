/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link IInvokeDispatcherFactory} represents a factory of {@link IInvokeDispatcher}.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInvokeDispatcherFactory {
    /**
     * Creates invoke dispatcher.
     *
     * @return invoke dispatcher
     */
    IInvokeDispatcher createDispatcher();
}
