/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.tester.ITestResultAnalyzer;


/**
 * The {@link TestResultAnalyzerConfiguration} is a configuration for test result analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TestResultAnalyzerConfiguration extends Configuration {
    public abstract ITestResultAnalyzer createAnalyzer();
}
