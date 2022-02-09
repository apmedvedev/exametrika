/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.config;

import com.exametrika.api.agent.config.AgentChannelConfiguration;
import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.config.ProfilerLoadContext;


/**
 * The {@link AgentConfigurationLoader} is a configuration loader for agent configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentConfigurationLoader extends TransportConfigurationLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        AgentLoadContext agentLoadContext = context.get(AgentConfiguration.SCHEMA);
        ProfilerLoadContext profilerLoadContext = context.get(ProfilerConfiguration.SCHEMA);

        String name = element.get("name", null);
        if (name == null) {
            if (System.getProperty("com.exametrika.hostAgent") != null)
                name = System.getProperty("com.exametrika.hostName");
            else
                name = System.getProperty("com.exametrika.nodeName");
        }

        JsonObject properties = element.get("properties");

        agentLoadContext.setChannel(loadChannel((JsonObject) element.get("channel"), context));
        agentLoadContext.setName(name);
        agentLoadContext.setComponent((String) element.get("component"));

        profilerLoadContext.setNodeName(name);
        profilerLoadContext.setNodeProperties(properties);
    }

    private AgentChannelConfiguration loadChannel(JsonObject element, ILoadContext context) {
        String serverHost = element.get("serverHost");
        long serverPort = element.get("serverPort");
        String bindAddress = element.get("bindAddress", null);
        boolean secured = element.get("secured");
        String keyStorePath = element.get("keyStorePath", null);
        String keyStorePassword = element.get("keyStorePassword", null);
        Long maxRate = element.get("maxRate", null);
        TransportConfiguration transport = loadTransport(element);
        return new AgentChannelConfiguration(serverHost, (int) serverPort, bindAddress, secured, keyStorePath,
                keyStorePassword, maxRate != null ? maxRate.intValue() : null, transport);
    }
}
