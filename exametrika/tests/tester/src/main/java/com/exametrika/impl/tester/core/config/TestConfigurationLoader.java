/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.tester.config.GenericTestNodeConfiguration;
import com.exametrika.api.tester.config.PlatformTestCaseBuilderConfiguration;
import com.exametrika.api.tester.config.PlatformTestCaseBuilderConfiguration.Format;
import com.exametrika.api.tester.config.SimpleTestActionConfiguration;
import com.exametrika.api.tester.config.SimpleTestCaseBuilderConfiguration;
import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.api.tester.config.TestConfiguration;
import com.exametrika.api.tester.config.TestStartStepConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.spi.tester.config.TestActionConfiguration;
import com.exametrika.spi.tester.config.TestCaseBuilderConfiguration;
import com.exametrika.spi.tester.config.TestNodeConfiguration;
import com.exametrika.spi.tester.config.TestReporterConfiguration;
import com.exametrika.spi.tester.config.TestResultAnalyzerConfiguration;


/**
 * The {@link TestConfigurationLoader} is a configuration loader for test configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestConfigurationLoader extends AbstractElementLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        TestLoadContext testLoadContext = context.get(TestConfiguration.SCHEMA);

        String installationPath = element.get("installationPath");
        String resultsPath = element.get("resultsPath");
        if (installationPath.startsWith("file:"))
            installationPath = installationPath.substring("file:".length());

        Set<String> roles = JsonUtils.toSet((JsonArray) element.get("roles"));
        Map<String, TestCaseConfiguration> testCases = loadTestCases((JsonObject) element.get("testCases"), context);

        testLoadContext.setInstallationPath(installationPath);
        testLoadContext.setRoles(roles);
        testLoadContext.setResultsPath(resultsPath);
        testLoadContext.setTestCases(testCases);
        testLoadContext.setReporters(loadReporters((JsonObject) element.get("reporters"), context));
    }

    private Map<String, TestCaseConfiguration> loadTestCases(JsonObject element, ILoadContext context) {
        Map<String, TestCaseConfiguration> map = new LinkedHashMap<String, TestCaseConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadTestCase(entry.getKey(), (JsonObject) entry.getValue(), context));
        return map;
    }

    private TestCaseConfiguration loadTestCase(String name, JsonObject element, ILoadContext context) {
        long duration = element.get("duration");
        Set<String> tags = JsonUtils.toSet((JsonArray) element.get("tags"));
        Map<String, TestNodeConfiguration> nodes = loadTestNodes((JsonObject) element.get("nodes"), context);
        List<TestStartStepConfiguration> startSteps = loadStartSteps((JsonArray) element.get("startSteps"));
        return new TestCaseConfiguration(name, nodes, startSteps, duration, tags);
    }

    private Map<String, TestNodeConfiguration> loadTestNodes(JsonObject element, ILoadContext context) {
        Map<String, TestNodeConfiguration> map = new LinkedHashMap<String, TestNodeConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadTestNode(entry.getKey(), (JsonObject) entry.getValue(), context));
        return map;
    }

    private List<TestStartStepConfiguration> loadStartSteps(JsonArray element) {
        List<TestStartStepConfiguration> list = new ArrayList<TestStartStepConfiguration>();
        for (Object child : element)
            list.add(loadStartStep((JsonObject) child));
        return list;
    }

    private List<TestResultAnalyzerConfiguration> loadAnalyzers(JsonArray element, ILoadContext context) {
        List<TestResultAnalyzerConfiguration> list = new ArrayList<TestResultAnalyzerConfiguration>();
        for (Object child : element)
            list.add((TestResultAnalyzerConfiguration) load(null, null, child, context));
        return list;
    }

    private TestCaseBuilderConfiguration loadTestCaseBuilder(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("PlatformTestCaseBuilder")) {
            String format = element.get("format");
            return new PlatformTestCaseBuilderConfiguration(Format.valueOf(format.toUpperCase()));
        } else if (type.equals("SimpleTestCaseBuilder"))
            return new SimpleTestCaseBuilderConfiguration();
        else
            return load(null, type, element, context);
    }

    private TestNodeConfiguration loadTestNode(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("GenericTestNode")) {
            Map<String, Object> properties = JsonUtils.toMap((JsonObject) element.get("properties"));
            String agent = element.get("agent");
            String role = element.get("role", null);
            Map<String, Object> executorParameters = JsonUtils.toMap((JsonObject) element.get("executorParameters"));
            String executorName = element.get("executorName");
            TestCaseBuilderConfiguration builder = loadTestCaseBuilder((JsonObject) element.get("builder"), context);
            Map<String, TestActionConfiguration> actions = loadActions((JsonObject) element.get("actions"), context);
            List<TestResultAnalyzerConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);
            return new GenericTestNodeConfiguration(name, properties, agent, role, executorName, executorParameters,
                    builder, actions, analyzers);
        } else
            return load(name, type, element, context);
    }

    private TestStartStepConfiguration loadStartStep(JsonObject element) {
        Set<String> nodes = JsonUtils.toSet((JsonArray) element.get("nodes"));
        long period = element.get("period");
        return new TestStartStepConfiguration(nodes, period);
    }

    private Map<String, TestActionConfiguration> loadActions(JsonObject element, ILoadContext context) {
        Map<String, TestActionConfiguration> map = new LinkedHashMap<String, TestActionConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadAction(entry.getKey(), (JsonObject) entry.getValue(), context));
        return map;
    }

    private TestActionConfiguration loadAction(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("SimpleTestAction")) {
            long startDelay = element.get("startDelay");
            boolean recurrent = element.get("type").equals("recurrent");
            long period = element.get("period");
            boolean random = element.get("random");
            Map<String, Object> parameters = JsonUtils.toMap((JsonObject) element.get("parameters"));
            return new SimpleTestActionConfiguration(name, startDelay, recurrent, period, random, parameters);
        } else
            return loadAction(name, element, context);
    }

    private Map<String, TestReporterConfiguration> loadReporters(JsonObject element, ILoadContext context) {
        Map<String, TestReporterConfiguration> map = new LinkedHashMap<String, TestReporterConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadReporter(entry.getKey(), (JsonObject) entry.getValue(), context));
        return map;
    }

    private TestReporterConfiguration loadReporter(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        return load(name, type, element, context);
    }
}
