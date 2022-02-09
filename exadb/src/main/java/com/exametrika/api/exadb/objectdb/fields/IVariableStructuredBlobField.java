/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.Iterator;

import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IVariableStructuredBlobField} represents a variable structured blob node field.
 *
 * @param <T> blob record element type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IVariableStructuredBlobField<T> extends IField {
    /**
     * Iterator on variable structured blob record elements.
     *
     * @param <T> blob record element type
     */
    interface IElementIterator<T> extends Iterator<T> {
        /**
         * Returns field.
         *
         * @return field
         */
        IVariableStructuredBlobField<T> getField();

        /**
         * Returns identifier of current element.
         *
         * @return identifier of current element
         */
        long getId();

        /**
         * Returns value of current element.
         *
         * @return value of current element
         */
        T get();

        /**
         * Sets new element.
         *
         * @param element element
         */
        void set(T element);

        /**
         * Positions iterator on element with specified identifier so call to {@link #next} will return this element.
         *
         * @param id identifier of new next element
         */
        void setNext(long id);
    }

    /**
     * Iterable on structured blob record elements.
     *
     * @param <T> blob record element type
     */
    interface IElementIterable<T> extends Iterable<T> {
        @Override
        IElementIterator<T> iterator();

        /**
         * Visits elements of iterable.
         *
         * @param condition condition to select elements, can be null if all elements are selected
         * @param visitor   visitor to visit elements
         */
        void visitElements(ICondition<T> condition, IVisitor<T> visitor);
    }

    /**
     * Iterator on variable structured blob records.
     *
     * @param <T> blob record element type
     */
    interface IRecordIterator<T> extends Iterator<IElementIterable<T>> {
        /**
         * Returns field.
         *
         * @return field
         */
        IVariableStructuredBlobField<T> getField();

        /**
         * Returns identifier of current record.
         *
         * @return identifier of current record
         */
        long getId();

        /**
         * Returns value of current record.
         *
         * @return value of current record
         */
        IElementIterable<T> get();

        /**
         * Positions iterator on record with specified identifier so call to {@link #next} will return this record.
         *
         * @param id identifier of new next record
         */
        void setNext(long id);
    }

    /**
     * Iterable on structured blob records.
     *
     * @param <T> blob record element type
     */
    interface IRecordIterable<T> extends Iterable<IElementIterable<T>> {
        @Override
        IRecordIterator<T> iterator();

        /**
         * Visits records of iterable.
         *
         * @param condition condition to select records, can be null if all records are selected
         * @param visitor   visitor to visit records
         */
        void visitRecords(ICondition<IElementIterable<T>> condition, IVisitor<IElementIterable<T>> visitor);
    }

    /**
     * Returns blob store node.
     *
     * @return blob store node or null if blob store node is not set
     */
    Object getStore();

    /**
     * Sets blob store node and creates blob to store field data. If field already has assigned blob store, deleting old blob.
     *
     * @param store blob store node
     */
    void setStore(Object store);

    /**
     * Returns blob record element by element identifier.
     *
     * @param elementId element identifier
     * @return blob record element
     */
    T getElement(long elementId);

    /**
     * Returns blob record elements by record identifier.
     *
     * @param recordId blob record identifier
     * @return blob record elements iterable
     */
    IElementIterable<T> getElements(long recordId);

    /**
     * Returns iterable on all blob records.
     *
     * @return iterable on all blob records
     */
    IRecordIterable<T> getRecords();

    /**
     * Adds new blob record.
     *
     * @return record identifier
     */
    long addRecord();

    /**
     * Adds new element to blob record.
     *
     * @param recordId identifier of record
     * @param element  blob record element
     * @return element identifier
     */
    long addElement(long recordId, T element);

    /**
     * Sets element.
     *
     * @param elementId identifier of element
     * @param element   blob record element
     */
    void setElement(long elementId, T element);

    /**
     * Clears record.
     *
     * @param recordId identifier of record
     */
    void clearRecord(long recordId);

    /**
     * Clears all blob contents.
     */
    void clear();
}
