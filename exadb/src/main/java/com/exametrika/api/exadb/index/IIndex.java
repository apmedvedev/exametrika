/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IIndex} represents an index.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndex {
    /**
     * Returns index schema configuration.
     *
     * @return index schema configuration
     */
    IndexSchemaConfiguration getSchema();

    /**
     * Returns unique identifier of index.
     *
     * @return unique identifier of index
     */
    int getId();

    /**
     * Is index stale?
     *
     * @return true if index is stale
     */
    boolean isStale();

    /**
     * Refreshes internal index cache position in order to prevent accidental unloading of unmodified or flushing of modified index
     * in big transaction.
     */
    void refresh();

    /**
     * Deletes index from database.
     */
    void delete();

    /**
     * Unloads index from memory.
     */
    void unload();
}
