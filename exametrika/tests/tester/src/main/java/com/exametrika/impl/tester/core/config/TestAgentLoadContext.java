/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import com.exametrika.api.tester.config.TestAgentChannelConfiguration;
import com.exametrika.api.tester.config.TestAgentConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TestAgentLoadContext} is a helper class that is used to load {@link TestAgentConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TestAgentLoadContext implements IContextFactory, IConfigurationFactory {
    private TestAgentChannelConfiguration channel;
    private String name;

    public void setChannel(TestAgentChannelConfiguration channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        if (channel != null)
            return new TestAgentConfiguration(name, channel);
        else
            return null;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new TestAgentLoadContext();
    }
}
