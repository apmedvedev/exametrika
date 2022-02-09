/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link IIndexOperationManager} represents an index operation manager of database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IIndexOperationManager {
    String NAME = IIndexOperationManager.class.getName();

    /**
     * Rebuilds index statistics.
     *
     * @param keyRatio         ratio between number of keys in statistics and number of keys in index in percents
     * @param rebuildThreshold minimal number of changes allowed to rebuild statistics
     * @param force            if true forces rebuilding statistics regardless of rebuild threshold
     */
    void rebuildStatistics(double keyRatio, long rebuildThreshold, boolean force);
}
