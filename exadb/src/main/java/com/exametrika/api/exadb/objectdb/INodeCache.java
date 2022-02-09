/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;


/**
 * The {@link INodeCache} represents a node cache.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeCache {
    /**
     * Returns cache category.
     *
     * @return cache category
     */
    public String getCacheCategory();

    /**
     * Returns cache category type configuration.
     *
     * @return cache category type configuration
     */
    CacheCategoryTypeConfiguration getConfiguration();

    /**
     * Returns current cache size.
     *
     * @return current cache size
     */
    long getCacheSize();

    /**
     * Returns maximum cache size.
     *
     * @return maximum cache size
     */
    long getMaxCacheSize();
}
