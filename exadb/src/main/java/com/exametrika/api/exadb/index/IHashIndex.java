/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link IHashIndex} represents an in-memory hash index.
 *
 * @param <K> key type
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHashIndex<K, V> extends IUniqueIndex<K, V> {
    /**
     * Returns index statistics.
     *
     * @return index statistics
     */
    HashIndexStatistics getStatistics();
}
