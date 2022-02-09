/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link IBehaviorTypeIdAllocator} represents an allocator of behavior type identifiers.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBehaviorTypeIdAllocator {
    /**
     * Allocates identifier of behavior type starting from 1.
     *
     * @return identifier of behavior type
     */
    int allocateTypeId();
}
