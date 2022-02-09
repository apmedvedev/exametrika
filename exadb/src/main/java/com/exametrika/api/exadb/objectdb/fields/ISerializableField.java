/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ISerializableField} represents a serializable node field.
 *
 * @param <T> serializable type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISerializableField<T> extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    T get();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(T value);
}
