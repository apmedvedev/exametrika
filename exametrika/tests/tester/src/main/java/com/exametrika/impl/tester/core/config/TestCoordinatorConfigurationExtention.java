/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import com.exametrika.api.tester.config.TestCoordinatorConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link TestCoordinatorConfigurationExtention} is a helper class that is used to load test coordinator configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TestCoordinatorConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("testCoordinator", new TestCoordinatorConfigurationLoader());
        parameters.contextFactories.put(TestCoordinatorConfiguration.SCHEMA, new TestCoordinatorLoadContext());
        parameters.schemaMappings.put(TestCoordinatorConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(TestCoordinatorConfiguration.class) + "/test-coordinator.schema", false));
        parameters.topLevelElements.put("testCoordinator", new Pair("TestCoordinator", false));
        return parameters;
    }
}
