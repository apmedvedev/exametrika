/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import com.exametrika.common.rawdb.IRawBatchControl;


/**
 * The {@link IBatchControl} represents a batch control interface.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBatchControl extends IRawBatchControl {
    /**
     * Returns true if caching is enabled. If caching is disabled all subsequently loaded cache elements will not be cached
     * and be returned as readonly.
     *
     * @return true if caching is enabled
     */
    boolean isCachingEnabled();

    /**
     * Enabled or disabled caching.
     *
     * @param value true if caching is enabled
     */
    void setCachingEnabled(boolean value);

    /**
     * Adds additional constraint on maximum cache size of specified cache category. Constraint is applied only when
     * batch step is running and disabled when normal transaction is executed. Cache category must be bound before
     * using this method.
     *
     * @param category cache category
     * @param value    maximum cache size of specified cache category
     */
    void setMaxCacheSize(String category, long value);
}
