/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import java.util.List;
import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestNodeConfiguration} is a configuration for test node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestNodeConfiguration extends Configuration {
    private final String name;
    private final String agent;
    private final String role;

    public TestNodeConfiguration(String name, String agent, String role) {
        Assert.notNull(name);
        Assert.notNull(agent);

        this.name = name;
        this.agent = agent;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getAgent() {
        return agent;
    }

    public String getRole() {
        return role;
    }

    public abstract String getExecutorName();

    public abstract Map<String, Object> getExecutorParameters();

    public abstract TestCaseBuilderConfiguration getTestCaseBuilder();

    public abstract Map<String, TestActionConfiguration> getActions();

    public abstract List<TestResultAnalyzerConfiguration> getAnalyzers();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestNodeConfiguration))
            return false;

        TestNodeConfiguration configuration = (TestNodeConfiguration) o;
        return name.equals(configuration.name) && agent.equals(configuration.agent) && Objects.equals(role, configuration.role);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, agent, role);
    }

    @Override
    public String toString() {
        return name;
    }
}
