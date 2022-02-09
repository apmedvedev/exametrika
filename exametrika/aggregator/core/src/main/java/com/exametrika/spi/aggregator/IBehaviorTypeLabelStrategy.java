/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IBehaviorTypeLabelStrategy} represents a behavior type label strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IBehaviorTypeLabelStrategy {
    /**
     * Returns behavior type label.
     *
     * @param context compute context
     * @return behavior type label
     */
    BehaviorType getBehaviorType(IComputeContext context);
}
