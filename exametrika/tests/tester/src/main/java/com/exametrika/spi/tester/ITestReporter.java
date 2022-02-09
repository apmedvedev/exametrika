/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.io.File;


/**
 * The {@link ITestReporter} is a test reporter.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestReporter {
    /**
     * Creates report in specified directory.
     *
     * @param resultsPath path to test results
     */
    void report(File resultsPath);
}
