/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.config;

import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link AgentConfigurationExtention} is a helper class that is used to load {@link AgentConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AgentConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("agent", new AgentConfigurationLoader());
        parameters.contextFactories.put(AgentConfiguration.SCHEMA, new AgentLoadContext());
        parameters.schemaMappings.put(AgentConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(AgentConfiguration.class) + "/agent.schema", false));
        parameters.schemaMappings.put(TransportConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(TransportConfiguration.class) + "/transport.schema", false));
        parameters.validators.put("agentChannel", new AgentChannelValidator());
        parameters.topLevelElements.put("agent", new Pair("Agent", false));
        return parameters;
    }
}
