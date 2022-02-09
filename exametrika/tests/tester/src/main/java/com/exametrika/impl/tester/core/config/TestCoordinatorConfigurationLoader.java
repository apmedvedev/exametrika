/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.tester.config.DirectTestAgentDiscoveryStrategyConfiguration;
import com.exametrika.api.tester.config.TestAgentConnectionConfiguration;
import com.exametrika.api.tester.config.TestCaseFilterConfiguration;
import com.exametrika.api.tester.config.TestCoordinatorChannelConfiguration;
import com.exametrika.api.tester.config.TestCoordinatorConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.spi.tester.config.TestAgentDiscoveryStrategyConfiguration;


/**
 * The {@link TestCoordinatorConfigurationLoader} is a configuration loader for test coordinator configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinatorConfigurationLoader extends AbstractElementLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        TestCoordinatorLoadContext coordinatorLoadContext = context.get(TestCoordinatorConfiguration.SCHEMA);

        String name = element.get("name");
        String testConfigurationPath = element.get("testConfigurationPath");
        List<TestCaseFilterConfiguration> execute = loadTestCaseFilters((JsonArray) element.get("execute"), context);

        coordinatorLoadContext.setChannel(loadChannel((JsonObject) element.get("channel"), context));
        coordinatorLoadContext.setName(name);
        coordinatorLoadContext.setExecute(execute);
        coordinatorLoadContext.setTestConfigurationPath(testConfigurationPath);
    }

    private List<TestAgentConnectionConfiguration> loadAgentConnections(JsonArray element) {
        List<TestAgentConnectionConfiguration> list = new ArrayList<TestAgentConnectionConfiguration>();
        for (Object child : element)
            list.add(loadAgentConnection((JsonObject) child));
        return list;
    }

    private TestCoordinatorChannelConfiguration loadChannel(JsonObject element, ILoadContext context) {
        TestAgentDiscoveryStrategyConfiguration discoveryStrategy = loadDiscoveryStrategy((JsonObject) element.get("discoveryStrategy"), context);
        String bindAddress = element.get("bindAddress", null);
        boolean secured = element.get("secured");
        String keyStorePath = element.get("keyStorePath", null);
        String keyStorePassword = element.get("keyStorePassword", null);
        return new TestCoordinatorChannelConfiguration(discoveryStrategy, bindAddress, secured, keyStorePath, keyStorePassword);
    }

    private TestAgentDiscoveryStrategyConfiguration loadDiscoveryStrategy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("DirectTestAgentDiscoveryStrategy")) {
            List<TestAgentConnectionConfiguration> agents = loadAgentConnections((JsonArray) element.get("agents"));
            return new DirectTestAgentDiscoveryStrategyConfiguration(agents);
        } else
            return load(null, type, element, context);
    }

    private TestAgentConnectionConfiguration loadAgentConnection(JsonObject element) {
        String name = element.get("name");
        String host = element.get("host");
        long port = element.get("port");
        Map<String, String> properties = JsonUtils.toMap((JsonObject) element.get("properties"));
        return new TestAgentConnectionConfiguration(name, host, (int) port, properties);
    }

    private List<TestCaseFilterConfiguration> loadTestCaseFilters(JsonArray element, ILoadContext context) {
        List<TestCaseFilterConfiguration> list = new ArrayList<TestCaseFilterConfiguration>();
        for (Object child : element)
            list.add(loadTestCaseFilter(child, context));
        return list;
    }

    private TestCaseFilterConfiguration loadTestCaseFilter(Object element, ILoadContext context) {
        if (element instanceof String)
            return new TestCaseFilterConfiguration(new NameFilter((String) element), null);

        JsonObject object = (JsonObject) element;
        NameFilter name = load(null, "NameFilter", object.get("name", null), context);
        NameFilter tag = load(null, "NameFilter", object.get("tag", null), context);
        return new TestCaseFilterConfiguration(name, tag);
    }
}
