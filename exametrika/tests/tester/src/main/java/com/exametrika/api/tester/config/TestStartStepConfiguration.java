/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestStartStepConfiguration} is a configuration for test start step.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestStartStepConfiguration extends Configuration {
    private final Set<String> nodes;
    private final long period;

    public TestStartStepConfiguration(Set<String> nodes, long period) {
        Assert.notNull(nodes);

        this.nodes = Immutables.wrap(nodes);
        this.period = period;
    }

    public Set<String> getNodes() {
        return nodes;
    }

    public long getPeriod() {
        return period;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestStartStepConfiguration))
            return false;

        TestStartStepConfiguration configuration = (TestStartStepConfiguration) o;
        return nodes.equals(configuration.nodes) && period == configuration.period;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodes, period);
    }

    @Override
    public String toString() {
        return nodes.toString();
    }
}
