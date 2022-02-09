/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;


/**
 * The {@link IUniqueIndex} represents an unique index.
 *
 * @param <K> key type
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUniqueIndex<K, V> extends IIndex {
    /**
     * Is index empty?
     *
     * @return true if index is empty
     */
    boolean isEmpty();

    /**
     * Returns number of index elements.
     *
     * @return number of index elements
     */
    long getCount();

    /**
     * Returns value associated with given key.
     *
     * @param key key
     * @return found value or null if value for given key is not found in index.
     * Value is valid until next index modification
     */
    V find(K key);

    /**
     * Adds new index element as key:value pair. If element with specified key already exists in index new value replaces
     * the old one.
     *
     * @param key   key
     * @param value value
     */
    void add(K key, V value);

    /**
     * Performs bulk add of specified elements.
     *
     * @param elements elements to add
     */
    void bulkAdd(Iterable<Pair<K, V>> elements);

    /**
     * Removes index element from index.
     *
     * @param key key
     */
    void remove(K key);

    /**
     * Removes all index elements.
     */
    void clear();

    /**
     * Normalizes key.
     *
     * @param key key
     * @return normalized key
     */
    ByteArray normalize(K key);
}
