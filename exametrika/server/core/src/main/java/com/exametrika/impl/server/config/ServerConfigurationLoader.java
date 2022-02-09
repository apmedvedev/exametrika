/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.config;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.server.config.ServerChannelConfiguration;
import com.exametrika.api.server.config.ServerConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.agent.config.TransportConfigurationLoader;
import com.exametrika.impl.profiler.config.ProfilerLoadContext;


/**
 * The {@link ServerConfigurationLoader} is a configuration loader for server configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerConfigurationLoader extends TransportConfigurationLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        ServerLoadContext loadContext = context.get(ServerConfiguration.SCHEMA);
        ProfilerLoadContext profilerLoadContext = context.get(ProfilerConfiguration.SCHEMA);

        String name = (String) element.get("name", null);
        String nodeName = name;
        if (name == null) {
            nodeName = System.getProperty("com.exametrika.hostName");
            name = nodeName + ".server";
        }
        loadContext.setName(name);
        loadContext.setChannel(loadChannel((JsonObject) element.get("channel"), context));
        loadContext.setDatabase((DatabaseConfiguration) load(null, "Database", element.get("database"), context));

        profilerLoadContext.setNodeName(nodeName);
    }

    private ServerChannelConfiguration loadChannel(JsonObject element, ILoadContext context) {
        long port = element.get("port");
        String bindAddress = element.get("bindAddress", null);
        boolean secured = element.get("secured");
        String keyStorePath = element.get("keyStorePath", null);
        String keyStorePassword = element.get("keyStorePassword", null);
        Long maxTotalRate = element.get("maxTotalRate", null);
        TransportConfiguration transport = loadTransport(element);
        return new ServerChannelConfiguration((int) port, bindAddress, secured, keyStorePath,
                keyStorePassword, maxTotalRate != null ? maxTotalRate.intValue() : null, transport);
    }
}
