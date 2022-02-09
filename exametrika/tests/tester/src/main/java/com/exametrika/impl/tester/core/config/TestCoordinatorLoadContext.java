/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import java.util.List;

import com.exametrika.api.tester.config.TestCaseFilterConfiguration;
import com.exametrika.api.tester.config.TestCoordinatorChannelConfiguration;
import com.exametrika.api.tester.config.TestCoordinatorConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TestCoordinatorLoadContext} is a helper class that is used to load {@link TestCoordinatorConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TestCoordinatorLoadContext implements IContextFactory, IConfigurationFactory {
    private TestCoordinatorChannelConfiguration channel;
    private String name;
    private List<TestCaseFilterConfiguration> execute;
    private String testConfigurationPath;

    public void setChannel(TestCoordinatorChannelConfiguration channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExecute(List<TestCaseFilterConfiguration> execute) {
        this.execute = execute;
    }

    public void setTestConfigurationPath(String value) {
        this.testConfigurationPath = value;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        if (channel != null)
            return new TestCoordinatorConfiguration(name, channel, execute, testConfigurationPath);
        else
            return null;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new TestCoordinatorLoadContext();
    }
}
