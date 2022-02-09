/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.ITestAction;


/**
 * The {@link TestActionConfiguration} is a configuration of test action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestActionConfiguration extends Configuration {
    private final String name;
    private final boolean recurrent;

    public TestActionConfiguration(String name, boolean recurrent) {
        Assert.notNull(name);

        this.name = name;
        this.recurrent = recurrent;
    }

    public String getName() {
        return name;
    }

    public boolean isRecurrent() {
        return recurrent;
    }

    public abstract ITestAction createAction(String nodeName);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestActionConfiguration))
            return false;

        TestActionConfiguration configuration = (TestActionConfiguration) o;
        return name.equals(configuration.name) && recurrent == configuration.recurrent;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, recurrent);
    }

    @Override
    public String toString() {
        return name;
    }
}
