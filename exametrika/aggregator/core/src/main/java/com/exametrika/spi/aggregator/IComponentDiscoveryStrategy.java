/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.List;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Pair;


/**
 * The {@link IComponentDiscoveryStrategy} represents a discovery strategy for component model.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentDiscoveryStrategy {
    /**
     * Called by aggregator when measurements for existing components has been discovered.
     *
     * @param existingComponents scope identifiers and metadata of existing components
     */
    void processDiscovered(List<Pair<Long, JsonObject>> existingComponents);
}
