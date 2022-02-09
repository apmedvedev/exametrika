/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;


/**
 * The {@link ChildResourceAllocatorConfigurationBuilder} is a builder of configuration of child resource allocator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ChildResourceAllocatorConfigurationBuilder extends ResourceAllocatorConfigurationBuilder<ChildResourceAllocatorConfigurationBuilder> {
    public ChildResourceAllocatorConfiguration toConfiguration() {
        return new ChildResourceAllocatorConfiguration(name, new LinkedHashMap<String, AllocationPolicyConfiguration>(policies),
                defaultPolicy, quotaIncreaseDelay, initializePeriod);
    }
}
