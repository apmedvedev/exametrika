/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import com.exametrika.api.tester.config.TestAgentConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link TestAgentConfigurationExtention} is a helper class that is used to load test configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TestAgentConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("testAgent", new TestAgentConfigurationLoader());
        parameters.contextFactories.put(TestAgentConfiguration.SCHEMA, new TestAgentLoadContext());
        parameters.schemaMappings.put(TestAgentConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(TestAgentConfiguration.class) + "/test-agent.schema", false));
        parameters.topLevelElements.put("testAgent", new Pair("TestAgent", false));
        return parameters;
    }
}
