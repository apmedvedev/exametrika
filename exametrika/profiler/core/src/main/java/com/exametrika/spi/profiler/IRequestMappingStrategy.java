/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IRequestMappingStrategy} is a request mapping strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IRequestMappingStrategy {
    /**
     * Called when request has begun. Maps raw request to internal represenation.
     *
     * @param scope   scope of request or null if scope is not used to map request
     * @param request raw request
     * @return request or null if request must be ignored
     */
    IRequest begin(IScope scope, Object request);

    /**
     * Returns request by variant. Used to get metadata and parameters of request.
     *
     * @param name    request name
     * @param variant mapping variant
     * @param request raw request
     * @return request
     */
    IRequest get(String name, int variant, Object request);

    /**
     * Called on timer.
     *
     * @param currentTime current time
     */
    void onTimer(long currentTime);
}
