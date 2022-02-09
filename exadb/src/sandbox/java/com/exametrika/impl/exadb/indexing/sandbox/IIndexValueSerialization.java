/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox;

import com.exametrika.common.io.IDataSerialization;


/**
 * The {@link IIndexValueSerialization} represents a helper object to serialize index values.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexValueSerialization extends IIndexValueDeserialization, IDataSerialization {
    /**
     * Removes all unused value areas starting from current position to the rest of the value.
     */
    void removeRest();

    /**
     * Creates cloned copy of current object.
     *
     * @return cloned copy of current object
     */
    IIndexValueSerialization clone();
}
