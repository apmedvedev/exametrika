/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.ITestAgentDiscoveryStrategy;
import com.exametrika.spi.tester.config.TestAgentDiscoveryStrategyConfiguration;


/**
 * The {@link DirectTestAgentDiscoveryStrategyConfiguration} is a configuration for direct test agent discovery strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DirectTestAgentDiscoveryStrategyConfiguration extends TestAgentDiscoveryStrategyConfiguration {
    private final List<TestAgentConnectionConfiguration> agents;

    public DirectTestAgentDiscoveryStrategyConfiguration(List<TestAgentConnectionConfiguration> agents) {
        Assert.notNull(agents);

        this.agents = Immutables.wrap(agents);
    }

    public List<TestAgentConnectionConfiguration> getAgents() {
        return agents;
    }

    @Override
    public ITestAgentDiscoveryStrategy createStrategy() {
        return new ITestAgentDiscoveryStrategy() {
            @Override
            public List<TestAgentConnectionConfiguration> discoverAgents() {
                return agents;
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DirectTestAgentDiscoveryStrategyConfiguration))
            return false;

        DirectTestAgentDiscoveryStrategyConfiguration configuration = (DirectTestAgentDiscoveryStrategyConfiguration) o;
        return agents.equals(configuration.agents);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(agents);
    }

    @Override
    public String toString() {
        return agents.toString();
    }
}
