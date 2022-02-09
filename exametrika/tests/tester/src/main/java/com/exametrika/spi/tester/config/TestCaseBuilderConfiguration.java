/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.tester.ITestCaseBuilder;


/**
 * The {@link TestCaseBuilderConfiguration} is a configuration for test case builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestCaseBuilderConfiguration extends Configuration {
    public abstract ITestCaseBuilder createBuilder();
}
