/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;


/**
 * The {@link IOperationManager} represents an operation manager of database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IOperationManager {
    /**
     * Creates snapshot of current spaces.
     *
     * @param snapshotDirectoryPath path to directory where snapshot will be created
     * @param completionHandler     completion handler or null if operation is performed synchronously
     */
    void snapshot(String snapshotDirectoryPath, ICompletionHandler completionHandler);

    /**
     * Creates dump of current spaces.
     *
     * @param dumpDirectoryPath path to directory where dump will be created
     * @param context           dump context
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void dump(String dumpDirectoryPath, IDumpContext context, ICompletionHandler completionHandler);

    /**
     * Backs up current spaces to specified store.
     *
     * @param archiveStore      archive store
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void backup(ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler);
}
