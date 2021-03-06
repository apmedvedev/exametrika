/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;


/**
 * The {@link DynamicUniformAllocationPolicyConfigurationBuilder} is a builder of configuration of dynamic uniform allocation policy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DynamicUniformAllocationPolicyConfigurationBuilder extends
        DynamicAllocationPolicyConfigurationBuilder<DynamicUniformAllocationPolicyConfigurationBuilder> {
    public DynamicUniformAllocationPolicyConfiguration toConfiguration() {
        return new DynamicUniformAllocationPolicyConfiguration(underloadedThresholdPercentage, overloadedThresholdPercentage,
                underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }
}
