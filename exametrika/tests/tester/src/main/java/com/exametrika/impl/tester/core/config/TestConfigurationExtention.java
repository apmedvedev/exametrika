/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import com.exametrika.api.tester.config.TestConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.tester.core.coordinator.TestMacroses;


/**
 * The {@link TestConfigurationExtention} is a helper class that is used to load test configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TestConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("test", new TestConfigurationLoader());
        parameters.contextFactories.put(TestConfiguration.SCHEMA, new TestLoadContext());
        parameters.schemaMappings.put(TestConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(TestConfiguration.class) + "/test.schema", false));
        parameters.topLevelElements.put("test", new Pair("Test", false));
        TestMacroses.register(parameters.macroses);
        return parameters;
    }
}
