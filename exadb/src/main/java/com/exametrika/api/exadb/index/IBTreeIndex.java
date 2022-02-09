/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link IBTreeIndex} represents a B+ Tree index.
 *
 * @param <K> key type
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBTreeIndex<K, V> extends ISortedIndex<K, V> {
    /**
     * Returns index statistics.
     *
     * @return index statistics
     */
    BTreeIndexStatistics getStatistics();
}
