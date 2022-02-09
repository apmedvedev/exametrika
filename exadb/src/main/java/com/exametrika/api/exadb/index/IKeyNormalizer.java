/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link IKeyNormalizer} is used to convert key value to binary form suitable for lexicographical comparisons.
 *
 * @param <K> input value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IKeyNormalizer<K> {
    /**
     * Converts key value to binary form suitable for lexicographical comparisons
     *
     * @param key key value
     * @return normalized binary representation of key
     */
    ByteArray normalize(K key);
}
