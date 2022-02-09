/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.spi.tester.config.TestNodeConfiguration;


/**
 * The {@link ITestCaseBuilder} is a test case builder.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestCaseBuilder {
    /**
     * Builds installation directory for specified test case and test node.
     *
     * @param installationPath path to test installation directory
     * @param buildPath        build path
     * @param testCase         test case configuration
     * @param node             test node configuration
     */
    void build(String installationPath, String buildPath, TestCaseConfiguration testCase, TestNodeConfiguration node);
}
