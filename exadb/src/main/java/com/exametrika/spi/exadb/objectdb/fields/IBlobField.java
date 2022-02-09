/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IBlobField} represents a blob node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBlobField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    IBlob get();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(IBlob value);
}
