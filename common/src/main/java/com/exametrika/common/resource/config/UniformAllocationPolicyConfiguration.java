/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.UniformAllocationPolicy;


/**
 * The {@link UniformAllocationPolicyConfiguration} is a configuration of uniform allocation policy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UniformAllocationPolicyConfiguration extends AllocationPolicyConfiguration {
    public UniformAllocationPolicyConfiguration() {
    }

    @Override
    public UniformAllocationPolicy createPolicy() {
        return new UniformAllocationPolicy();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        else if (!(o instanceof UniformAllocationPolicyConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
