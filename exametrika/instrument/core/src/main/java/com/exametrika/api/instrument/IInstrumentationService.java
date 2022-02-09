/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;


/**
 * The {@link IInstrumentationService} represents an instrumentation service.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInstrumentationService {
    final String NAME = "instrumentation";

    /**
     * Returns join point provider.
     *
     * @return join point provider
     */
    IJoinPointProvider getJoinPointProvider();

    /**
     * Returns class transformer.
     *
     * @return class transformer or null if class transformer is not avaliable
     */
    IClassTransformer getClassTransformer();

    /**
     * Adds join point filter. Must be called in wiring phase of service initialization.
     *
     * @param filter join point filter
     */
    void addJoinPointFilter(IJoinPointFilter filter);

    /**
     * Sets reentrancy listener.
     *
     * @param listener reentrancy listener
     */
    void setReentrancyListener(IReentrancyListener listener);
}
