/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.config.TestNodeConfiguration;


/**
 * The {@link TestCaseConfiguration} is a configuration for test case.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCaseConfiguration extends Configuration {
    private final String name;
    private final Map<String, TestNodeConfiguration> nodes;
    private final List<TestStartStepConfiguration> startSteps;
    private final long duration;
    private final Set<String> tags;

    public TestCaseConfiguration(String name, Map<String, TestNodeConfiguration> nodes,
                                 List<TestStartStepConfiguration> startSteps, long duration, Set<String> tags) {
        Assert.notNull(name);
        Assert.notNull(nodes);
        Assert.notNull(startSteps);
        Assert.notNull(tags);

        this.name = name;
        this.nodes = Immutables.wrap(nodes);
        this.startSteps = Immutables.wrap(startSteps);
        this.duration = duration;
        this.tags = Immutables.wrap(tags);
    }

    public String getName() {
        return name;
    }

    public Map<String, TestNodeConfiguration> getNodes() {
        return nodes;
    }

    public List<TestStartStepConfiguration> getStartSteps() {
        return startSteps;
    }

    public long getDuration() {
        return duration;
    }

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestCaseConfiguration))
            return false;

        TestCaseConfiguration configuration = (TestCaseConfiguration) o;
        return name.equals(configuration.name) && nodes.equals(configuration.nodes) &&
                startSteps.equals(configuration.startSteps) && duration == configuration.duration && tags.equals(configuration.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, nodes, startSteps, duration, tags);
    }

    @Override
    public String toString() {
        return name;
    }
}
