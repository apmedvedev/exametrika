/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link IObjectOperationManager} represents an object operation manager of database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectOperationManager {
    String NAME = IObjectOperationManager.class.getName();

    /**
     * Compacts database.
     *
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void compact(ICompletionHandler completionHandler);
}
