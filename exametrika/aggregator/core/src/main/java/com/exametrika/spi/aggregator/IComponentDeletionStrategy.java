/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.Set;


/**
 * The {@link IComponentDeletionStrategy} represents a deletion strategy for dynamic components.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentDeletionStrategy {
    /**
     * Called by aggregator when measurements for existing components has been discovered.
     *
     * @param existingComponents scope identifiers of existing components
     */
    void processDeleted(Set<Long> existingComponents);
}
