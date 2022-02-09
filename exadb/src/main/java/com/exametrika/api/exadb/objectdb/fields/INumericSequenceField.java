/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link INumericSequenceField} represents a numeric sequence field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INumericSequenceField extends IField {
    /**
     * Returns next sequence value.
     *
     * @return next sequence value
     */
    long getNext();

    /**
     * Returns field value.
     *
     * @return field value
     */
    long getLong();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setLong(long value);
}
