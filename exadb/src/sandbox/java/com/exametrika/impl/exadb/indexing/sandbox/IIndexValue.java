/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox;


/**
 * The {@link IIndexValue} represents an index value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexValue {
    /**
     * Creates value serialization object.
     *
     * @return value serialization object
     */
    IIndexValueSerialization createSerialization();

    /**
     * Creates value deserialization object.
     *
     * @return value deserialization object
     */
    IIndexValueDeserialization createDeserialization();
}
