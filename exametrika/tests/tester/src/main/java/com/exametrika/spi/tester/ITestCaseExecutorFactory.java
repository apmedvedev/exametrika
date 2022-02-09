/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.util.Map;

import com.exametrika.common.compartment.ICompartment;


/**
 * The {@link ITestCaseExecutorFactory} is a test case executor factory.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestCaseExecutorFactory {
    /**
     * Returns test case executor name.
     *
     * @return test case executor name
     */
    String getName();

    /**
     * Creates test case executor.
     *
     * @param path        installation path
     * @param parameters  test case parameters
     * @param compartment compartment
     * @return test case executor
     */
    ITestCaseExecutor createExecutor(String path, Map<String, Object> parameters, ICompartment compartment);
}
