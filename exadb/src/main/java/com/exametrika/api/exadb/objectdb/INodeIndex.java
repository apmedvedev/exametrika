/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;


/**
 * The {@link INodeIndex} represents a node index.
 *
 * @param <K> key type
 * @param <V> node type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeIndex<K, V> {
    /**
     * Is value associated with given key contained in the index.
     *
     * @param key key
     * @return if true value for given key is contained in the index.
     */
    boolean contains(K key);

    /**
     * Returns value associated with given key.
     *
     * @param key key
     * @return found value or null if value for given key is not found in index.
     * Value is valid until next index modification
     */
    V find(K key);
}
