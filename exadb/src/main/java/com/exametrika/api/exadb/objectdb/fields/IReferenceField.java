/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.Iterator;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IReferenceField} represents a reference node field.
 *
 * @param <T> referent node type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IReferenceField<T> extends IField, Iterable<T> {
    /**
     * Reference iterator.
     *
     * @param <T>
     */
    interface IReferenceIterator<T> extends Iterator<T> {
        /**
         * Returns flags of current reference.
         *
         * @return flags of current reference
         */
        int getFlags();
    }

    /**
     * Returns iterator over field values.
     *
     * @return iterator over field values
     */
    @Override
    IReferenceIterator<T> iterator();

    /**
     * Adds field value.
     *
     * @param value field value
     */
    void add(T value);

    /**
     * Adds field value.
     *
     * @param value field value
     * @param flags arbitrary flags associated with reference
     */
    void add(T value, int flags);

    /**
     * Removes field value
     *
     * @param value field value
     */
    void remove(T value);

    /**
     * Removes all references.
     */
    void clear();
}
