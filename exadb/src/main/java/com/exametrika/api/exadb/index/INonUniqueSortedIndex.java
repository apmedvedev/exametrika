/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link INonUniqueSortedIndex} represents a sorted index which can hold duplicate keys (each key:value pair still must have to be unique).
 *
 * @param <K> key type
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INonUniqueSortedIndex<K, V> extends ISortedIndex<K, V> {
    /**
     * Returns first value associated with given key.
     *
     * @param key key. Value is valid until next index modification
     * @return found value or null if value for given key is not found in index
     */
    @Override
    V find(K key);

    /**
     * Returns values associated with given key.
     *
     * @param key key. Value is valid until next index modification
     * @return iterable of index values for specified key. Returned iterable is valid until
     * next index modification
     */
    Iterable<V> findValues(K key);

    /**
     * Removes all index elements for given key from index.
     *
     * @param key key
     */
    @Override
    void remove(K key);

    /**
     * Removes index element for given key and value from index.
     *
     * @param key   key
     * @param value value
     */
    void remove(K key, V value);
}
