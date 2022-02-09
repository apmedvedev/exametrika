/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import java.util.List;
import java.util.Set;

import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.spi.aggregator.config.schema.ArchivePolicySchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.TruncationPolicySchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;


/**
 * The {@link IPeriodOperationManager} represents an object operation manager of database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodOperationManager {
    String NAME = IPeriodOperationManager.class.getName();

    /**
     * Archives period cycles.
     *
     * @param spaceFilter       space alias filter or null if all spaces are used
     * @param periods           period aliases or null if all periods are used (for period spaces only)
     * @param archivePolicy     archive policy
     * @param archiveStore      archive store
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void archiveCycles(NameFilter spaceFilter, List<String> periods, ArchivePolicySchemaConfiguration archivePolicy,
                       ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler);

    /**
     * Truncate old period cycles.
     *
     * @param spaceFilter       space alias filter or null if all spaces are used
     * @param periods           period aliases or null if all periods are used (for period spaces only)
     * @param truncationPolicy  truncation policy
     * @param ignoreRestored    if true all manually restored cycles are ignored
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void truncateCycles(NameFilter spaceFilter, List<String> periods, TruncationPolicySchemaConfiguration truncationPolicy,
                        boolean ignoreRestored, ICompletionHandler completionHandler);

    /**
     * Restores files of cycles with specified identifiers from given archive storage.
     *
     * @param cycleIds          identifiers of restored cycles
     * @param archiveStore      archive store
     * @param completionHandler completion handler or null if operation is performed synchronously
     */
    void restoreCycles(Set<String> cycleIds, ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler);
}
