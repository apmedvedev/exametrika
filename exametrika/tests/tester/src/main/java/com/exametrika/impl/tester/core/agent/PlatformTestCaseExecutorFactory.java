/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.spi.tester.ITestCaseExecutor;
import com.exametrika.spi.tester.ITestCaseExecutorFactory;


/**
 * The {@link PlatformTestCaseExecutorFactory} represents a platform test case executor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformTestCaseExecutorFactory implements ITestCaseExecutorFactory {
    @Override
    public String getName() {
        return "platform";
    }

    @Override
    public ITestCaseExecutor createExecutor(String path, Map<String, Object> parameters, ICompartment compartment) {
        return new PlatformTestCaseExecutor(path, parameters, compartment);
    }
}
