/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import com.exametrika.api.tester.config.TestAgentChannelConfiguration;
import com.exametrika.api.tester.config.TestAgentConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link TestAgentConfigurationLoader} is a configuration loader for test agent configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentConfigurationLoader extends AbstractElementLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        TestAgentLoadContext agentLoadContext = context.get(TestAgentConfiguration.SCHEMA);

        String name = element.get("name");

        agentLoadContext.setChannel(loadChannel((JsonObject) element.get("channel"), context));
        agentLoadContext.setName(name);
    }

    private TestAgentChannelConfiguration loadChannel(JsonObject element, ILoadContext context) {
        long port = element.get("port");
        String bindAddress = element.get("bindAddress", null);
        boolean secured = element.get("secured");
        String keyStorePath = element.get("keyStorePath", null);
        String keyStorePassword = element.get("keyStorePassword", null);
        return new TestAgentChannelConfiguration((int) port, bindAddress, secured, keyStorePath, keyStorePassword);
    }
}
