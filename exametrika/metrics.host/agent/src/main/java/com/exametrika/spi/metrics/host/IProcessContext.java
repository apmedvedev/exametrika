/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.host;

import java.util.List;
import java.util.Map;


/**
 * The {@link IProcessContext} represents a context of some host process.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProcessContext {
    /**
     * Returns identifier of process.
     *
     * @return identifier of process
     */
    long getId();

    /**
     * Returns identifier of parent process.
     *
     * @return identifier of parent process
     */
    long getParentId();

    /**
     * Returns name of process.
     *
     * @return name of process
     */
    String getName();

    /**
     * Returns command of process.
     *
     * @return command of process
     */
    String getCommand();

    /**
     * Returns working directory of process.
     *
     * @return working directory of process
     */
    String getWorkingDir();

    /**
     * Returns arguments of process.
     *
     * @return arguments of process
     */
    String[] getArgs();

    /**
     * Returns environment of process.
     *
     * @return environment of process
     */
    Map<String, String> getEnvironment();

    /**
     * Returns user of process.
     *
     * @return user of process
     */
    String getUser();

    /**
     * Returns group of process.
     *
     * @return group of process
     */
    String getGroup();

    /**
     * Returns modules of process.
     *
     * @return modules of process
     */
    List<String> getModules();

    /**
     * Returns priority of process.
     *
     * @return priority of process
     */
    int getPriority();

    /**
     * Returns start time of process.
     *
     * @return start time of process
     */
    long getStartTime();
}
