/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.List;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestCoordinatorConfiguration} is a configuration for test coordinator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinatorConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.tester.coordinator-1.0";
    private final String name;
    private final TestCoordinatorChannelConfiguration channel;
    private final List<TestCaseFilterConfiguration> execute;
    private final String testConfigurationPath;

    public TestCoordinatorConfiguration(String name, TestCoordinatorChannelConfiguration channel, List<TestCaseFilterConfiguration> execute,
                                        String testConfigurationPath) {
        Assert.notNull(name);
        Assert.notNull(channel);
        Assert.notNull(execute);
        Assert.notNull(testConfigurationPath);

        this.name = name;
        this.channel = channel;
        this.execute = Immutables.wrap(execute);
        this.testConfigurationPath = testConfigurationPath;
    }

    public String getName() {
        return name;
    }

    public TestCoordinatorChannelConfiguration getChannel() {
        return channel;
    }

    public List<TestCaseFilterConfiguration> getExecute() {
        return execute;
    }

    public String getTestConfigurationPath() {
        return testConfigurationPath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestCoordinatorConfiguration))
            return false;

        TestCoordinatorConfiguration configuration = (TestCoordinatorConfiguration) o;
        return name.equals(configuration.name) && channel.equals(configuration.channel) && execute.equals(configuration.execute) &&
                testConfigurationPath.equals(configuration.testConfigurationPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, channel, execute, testConfigurationPath);
    }

    @Override
    public String toString() {
        return name;
    }
}
