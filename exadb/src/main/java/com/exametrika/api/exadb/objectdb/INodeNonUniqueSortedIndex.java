/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;


/**
 * The {@link INodeNonUniqueSortedIndex} represents a sorted node index which can hold duplicate keys (each key:value pair still must have to be unique).
 *
 * @param <K> key type
 * @param <V> node type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeNonUniqueSortedIndex<K, V> extends INodeSortedIndex<K, V> {
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
}
