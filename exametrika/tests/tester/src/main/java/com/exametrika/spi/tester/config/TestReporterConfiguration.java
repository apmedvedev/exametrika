/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.ITestReporter;


/**
 * The {@link TestReporterConfiguration} is a configuration for test reporter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestReporterConfiguration extends Configuration {
    private final String name;

    public TestReporterConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract ITestReporter createReporter();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestReporterConfiguration))
            return false;

        TestReporterConfiguration configuration = (TestReporterConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
