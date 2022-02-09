/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.config.TestActionConfiguration;
import com.exametrika.spi.tester.config.TestCaseBuilderConfiguration;
import com.exametrika.spi.tester.config.TestNodeConfiguration;
import com.exametrika.spi.tester.config.TestResultAnalyzerConfiguration;


/**
 * The {@link GenericTestNodeConfiguration} is a configuration for test node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GenericTestNodeConfiguration extends TestNodeConfiguration {
    private final String name;
    private final Map<String, Object> properties;
    private final String agent;
    private final String executorName;
    private final Map<String, Object> executorParameters;
    private final TestCaseBuilderConfiguration testCaseBuilder;
    private final Map<String, TestActionConfiguration> actions;
    private final List<TestResultAnalyzerConfiguration> analyzers;

    public GenericTestNodeConfiguration(String name, Map<String, Object> properties, String agent, String role,
                                        String executorName, Map<String, Object> executorParameters, TestCaseBuilderConfiguration testCaseBuilder,
                                        Map<String, TestActionConfiguration> actions, List<TestResultAnalyzerConfiguration> analyzers) {
        super(name, agent, role);

        Assert.notNull(properties);
        Assert.notNull(executorName);
        Assert.notNull(executorParameters);
        Assert.notNull(testCaseBuilder);
        Assert.notNull(actions);
        Assert.notNull(analyzers);

        this.name = name;
        this.properties = Immutables.wrap(properties);
        this.agent = agent;
        this.executorName = executorName;
        this.executorParameters = Immutables.wrap(executorParameters);
        this.testCaseBuilder = testCaseBuilder;
        this.actions = Immutables.wrap(actions);
        this.analyzers = Immutables.wrap(analyzers);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String getExecutorName() {
        return executorName;
    }

    @Override
    public Map<String, Object> getExecutorParameters() {
        return executorParameters;
    }

    @Override
    public TestCaseBuilderConfiguration getTestCaseBuilder() {
        return testCaseBuilder;
    }

    @Override
    public Map<String, TestActionConfiguration> getActions() {
        return actions;
    }

    @Override
    public List<TestResultAnalyzerConfiguration> getAnalyzers() {
        return analyzers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GenericTestNodeConfiguration))
            return false;

        GenericTestNodeConfiguration configuration = (GenericTestNodeConfiguration) o;
        return name.equals(configuration.name) && properties.equals(configuration.properties) &&
                agent.equals(configuration.agent) && executorName.equals(configuration.executorName) &&
                executorParameters.equals(configuration.executorParameters) && testCaseBuilder.equals(configuration.testCaseBuilder) &&
                actions.equals(configuration.actions) && analyzers.equals(configuration.analyzers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, properties, agent, executorName, executorParameters, testCaseBuilder, actions, analyzers);
    }
}
