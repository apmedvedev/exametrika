/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;


/**
 * The {@link IJoinPointFilter} represents a join point filter.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJoinPointFilter {
    /**
     * Matches specified join point.
     *
     * @param joinPoint join point to match
     * @return true if join point matches againts this filter
     */
    boolean match(IJoinPoint joinPoint);
}
