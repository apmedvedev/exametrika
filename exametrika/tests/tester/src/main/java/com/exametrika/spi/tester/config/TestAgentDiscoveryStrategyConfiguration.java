/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.tester.ITestAgentDiscoveryStrategy;


/**
 * The {@link TestAgentDiscoveryStrategyConfiguration} is a configuration for test agent discovery strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestAgentDiscoveryStrategyConfiguration extends Configuration {
    public abstract ITestAgentDiscoveryStrategy createStrategy();
}
