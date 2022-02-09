/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.impl.tester.core.coordinator.SimpleTestCaseBuilder;
import com.exametrika.spi.tester.ITestCaseBuilder;
import com.exametrika.spi.tester.config.TestCaseBuilderConfiguration;


/**
 * The {@link SimpleTestCaseBuilderConfiguration} is a configuration for simple test case builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTestCaseBuilderConfiguration extends TestCaseBuilderConfiguration {
    public SimpleTestCaseBuilderConfiguration() {
    }

    @Override
    public ITestCaseBuilder createBuilder() {
        return new SimpleTestCaseBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleTestCaseBuilderConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
