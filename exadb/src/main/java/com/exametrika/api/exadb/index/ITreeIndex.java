/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link ITreeIndex} represents an in-memory tree index.
 *
 * @param <K> key type
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITreeIndex<K, V> extends ISortedIndex<K, V> {
    /**
     * Returns index statistics.
     *
     * @return index statistics
     */
    TreeIndexStatistics getStatistics();
}
