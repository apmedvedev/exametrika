/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.agent.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AgentConfiguration} is a configuration of agent.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.agent-1.0";
    private final AgentChannelConfiguration channel;
    private final String name;
    private final String component;

    public AgentConfiguration(AgentChannelConfiguration channel, String name, String component) {
        Assert.notNull(name);
        Assert.notNull(channel);
        Assert.notNull(component);

        this.channel = channel;
        this.name = name;
        this.component = component;
    }

    public AgentChannelConfiguration getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    public String getComponent() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AgentConfiguration))
            return false;

        AgentConfiguration configuration = (AgentConfiguration) o;
        return channel.equals(configuration.channel) && name.equals(configuration.name) && component.equals(configuration.component);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channel, name, component);
    }

    @Override
    public String toString() {
        return name;
    }
}
