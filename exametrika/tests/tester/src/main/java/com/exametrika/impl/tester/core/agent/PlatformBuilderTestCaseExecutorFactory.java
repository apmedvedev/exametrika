/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.spi.tester.ITestCaseExecutor;
import com.exametrika.spi.tester.ITestCaseExecutorFactory;


/**
 * The {@link PlatformBuilderTestCaseExecutorFactory} represents a platform builder test case executor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformBuilderTestCaseExecutorFactory implements ITestCaseExecutorFactory {
    @Override
    public String getName() {
        return "platformBuilder";
    }

    @Override
    public ITestCaseExecutor createExecutor(String path, Map<String, Object> parameters, ICompartment compartment) {
        return new PlatformBuilderTestCaseExecutor(path, parameters, compartment);
    }
}
