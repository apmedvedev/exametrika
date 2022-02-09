/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link IValueConverter} represents a value converter.
 *
 * @param <V> value type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IValueConverter<V> {
    /**
     * Converts value to the byte array.
     *
     * @param value value
     * @return byte array
     */
    ByteArray toByteArray(V value);

    /**
     * Converts byte array to the value.
     *
     * @param buffer byte array
     * @return value
     */
    V toValue(ByteArray buffer);
}
