/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;


/**
 * The {@link IIndexListener} represents an index listener.
 *
 * @param <V> valie type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexListener<V> {
    /**
     * Called when entry with specified value is removed.
     *
     * @param value value
     */
    void onRemoved(V value);

    /**
     * Called when all entries are removed.
     */
    void onCleared();
}
