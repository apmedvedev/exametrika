/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IPrimaryField} represents a primary node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IPrimaryField extends IField {
    /**
     * Returns node's primary key.
     *
     * @return node's primary key
     */
    Object getKey();
}
