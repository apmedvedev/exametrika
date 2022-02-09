/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.io.File;


/**
 * The {@link ITestResultAnalyzer} is a test result analyzer.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestResultAnalyzer {
    /**
     * Analyzes test results of a test node.
     *
     * @param role        test node role
     * @param resultsPath path to test results
     * @return true if test has succeeded, false if test has failed
     */
    boolean analyze(File resultsPath);
}
