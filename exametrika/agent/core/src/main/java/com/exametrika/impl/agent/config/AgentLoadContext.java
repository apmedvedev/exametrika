/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.config;

import com.exametrika.api.agent.config.AgentChannelConfiguration;
import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AgentLoadContext} is a helper class that is used to load {@link AgentConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AgentLoadContext implements IContextFactory, IConfigurationFactory {
    private AgentChannelConfiguration channel;
    private String name;
    private String component;

    public void setChannel(AgentChannelConfiguration channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setComponent(String component) {
        Assert.notNull(component);

        this.component = component;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        if (component != null)
            return new AgentConfiguration(channel, name, component);
        else
            return null;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new AgentLoadContext();
    }
}
