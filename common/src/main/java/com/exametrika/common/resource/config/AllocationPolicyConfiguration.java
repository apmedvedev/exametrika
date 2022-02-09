/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.resource.IAllocationPolicy;


/**
 * The {@link AllocationPolicyConfiguration} is a configuration of allocation policy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AllocationPolicyConfiguration extends Configuration {
    public abstract IAllocationPolicy createPolicy();
}
