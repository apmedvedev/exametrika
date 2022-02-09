/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.util.List;

import com.exametrika.api.tester.config.TestAgentConnectionConfiguration;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link ITestAgentDiscoveryStrategy} is a test agent discovery strategy.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestAgentDiscoveryStrategy extends ILifecycle {
    /**
     * Discovers test agents.
     *
     * @return list of test agent connection configurations
     */
    List<TestAgentConnectionConfiguration> discoverAgents();
}
