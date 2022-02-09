/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.tester.core.coordinator.SimpleTestAction;
import com.exametrika.spi.tester.ITestAction;
import com.exametrika.spi.tester.config.TestActionConfiguration;


/**
 * The {@link SimpleTestActionConfiguration} is a configuration of simple test action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTestActionConfiguration extends TestActionConfiguration {
    private final long startDelay;
    private final long period;
    private final boolean random;
    private final Map<String, Object> parameters;

    public SimpleTestActionConfiguration(String name, long startDelay, boolean recurrent, long period, boolean random,
                                         Map<String, Object> parameters) {
        super(name, recurrent);

        Assert.notNull(parameters);

        this.startDelay = startDelay;
        this.period = period;
        this.random = random;
        this.parameters = Immutables.wrap(parameters);
    }

    public long getStartDelay() {
        return startDelay;
    }

    public long getPeriod() {
        return period;
    }

    public boolean isRandom() {
        return random;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public ITestAction createAction(String nodeName) {
        return new SimpleTestAction(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleTestActionConfiguration))
            return false;

        SimpleTestActionConfiguration configuration = (SimpleTestActionConfiguration) o;
        return super.equals(configuration) && startDelay == configuration.startDelay &&
                period == configuration.period && random == configuration.random && parameters.equals(configuration.parameters);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(startDelay, period, random, parameters);
    }
}
