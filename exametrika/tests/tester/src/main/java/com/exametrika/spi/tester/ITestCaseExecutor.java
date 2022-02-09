/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.tester;

import java.io.File;
import java.util.Map;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link ITestCaseExecutor} is a test case executor.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITestCaseExecutor {
    enum State {
        /**
         * Process is running.
         */
        RUNNING,
        /**
         * Process has been succeeded.
         */
        SUCCEEDED,
        /**
         * Process has been failed.
         */
        FAILED,
        /**
         * Process has been intentially stopped by testcase.
         */
        STOPPED
    }

    /**
     * Returns process under test state?
     *
     * @return process state
     */
    State getState();

    /**
     * Returns console file.
     *
     * @return console file
     */
    File getConsole();

    /**
     * Performs custom installation steps.
     *
     * @param path path to installation file received from server
     */
    void install(String path);

    /**
     * Starts process.
     *
     * @param completionHandler completion handler, if null start action is executed synchronously
     */
    void start(ICompletionHandler completionHandler);

    /**
     * Stops process.
     *
     * @param completionHandler completion handler, if null stop action is executed synchronously
     */
    void stop(ICompletionHandler completionHandler);

    /**
     * Stop process (if it is alive) and collects test results to specified path and destroys test case executor.
     *
     * @param path path where test results must be collected. Can be null if results are not collected
     */
    void destroy(String path);

    /**
     * Executes action with specified name and parameters.
     *
     * @param action            action name
     * @param parameters        action parameters. Can be null if they are not used
     * @param completionHandler completion handler, if null action is executed synchronously
     */
    void execute(String action, Map<String, Object> parameters, ICompletionHandler completionHandler);
}
