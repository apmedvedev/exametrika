/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;

import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.tester.ITestCaseBuilder;
import com.exametrika.spi.tester.config.TestNodeConfiguration;


/**
 * The {@link SimpleTestCaseBuilder} is a simple test case builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTestCaseBuilder implements ITestCaseBuilder {
    @Override
    public void build(String installationPath, String buildPath, TestCaseConfiguration testCase, TestNodeConfiguration node) {
        if (node.getRole() == null)
            Files.copy(new File(installationPath), new File(buildPath));
    }
}
