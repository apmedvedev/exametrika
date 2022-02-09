/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.agent;

import java.io.File;
import java.util.List;

import com.exametrika.common.services.IService;


/**
 * The {@link IActionContext} is an action context.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IActionContext {
    /**
     * Returns action to execute.
     *
     * @param <T> action type
     * @return action to execute
     */
    <T> T getAction();

    /**
     * Finds service by name.
     *
     * @param name service name
     * @return service or null if service is not found
     */
    <T extends IService> T findService(String name);

    /**
     * Sends result of action execution.
     *
     * @param result result of action execution
     * @param files  list of files or null if files are not used
     */
    void sendResult(Object result, List<File> files);

    /**
     * Sends error of action execution.
     *
     * @param error error
     */
    void sendError(Throwable error);
}
