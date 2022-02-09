/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.config;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.server.config.ServerConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ServerConfigurationExtention} is a helper class that is used to load {@link ServerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ServerConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("server", new ServerConfigurationLoader());
        parameters.contextFactories.put(ServerConfiguration.SCHEMA, new ServerLoadContext());
        parameters.schemaMappings.put(ServerConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ServerConfiguration.class) + "/server.schema", false));
        parameters.schemaMappings.put(TransportConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(TransportConfiguration.class) + "/transport.schema", false));
        parameters.validators.put("serverChannel", new ServerChannelValidator());
        parameters.topLevelElements.put("server", new Pair("Server", false));
        return parameters;
    }
}
