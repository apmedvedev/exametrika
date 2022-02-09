/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox;

import com.exametrika.api.exadb.index.IUniqueIndex;


/**
 * The {@link IIndexValueSpace} represents a index value space. Index value space contains variable-sized streams of data -
 * values which contained in index. Value consists of two parts - index part and overflow part. Index part is fixed and
 * stored directly in index. Overflow part is variable-sized and stored in separate file as linked list of
 * areas of fixed size.
 * <p>
 * In operations with values value is valid until next index modification. Also all values received using {@link IUniqueIndex} are
 * read only.
 *
 * @param <K> key type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexValueSpace<K> {
    /**
     * Returns index.
     *
     * @return index
     */
    IUniqueIndex<K, IIndexValue> getIndex();

    /**
     * Creates a new value. Value is writable but in index stored only those modifications of value made before value is added
     * into the index.
     *
     * @param initialSize initial size of value stored in BTree index
     * @return value
     */
    IIndexValue createValue(int initialSize);

    /**
     * Finds value by specified key.
     *
     * @param key      key
     * @param readOnly if false value is writable
     * @return value or null if value is not found
     */
    IIndexValue find(K key, boolean readOnly);
}
