/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestAgentConfiguration} is a configuration of test agent.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.tester.agent-1.0";
    private final String name;
    private final TestAgentChannelConfiguration channel;

    public TestAgentConfiguration(String name, TestAgentChannelConfiguration channel) {
        Assert.notNull(name);
        Assert.notNull(channel);

        this.name = name;
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public TestAgentChannelConfiguration getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestAgentConfiguration))
            return false;

        TestAgentConfiguration configuration = (TestAgentConfiguration) o;
        return name.equals(configuration.name) && channel.equals(configuration.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, channel);
    }

    @Override
    public String toString() {
        return name;
    }
}
