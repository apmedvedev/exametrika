/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.Iterator;

import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link IStructuredBlobField} represents a structured blob node field.
 *
 * @param <T> blob record type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStructuredBlobField<T> extends IField {
    /**
     * Iterator on structured blob records.
     *
     * @param <T> blob record type
     */
    interface IStructuredIterator<T> extends Iterator<T> {
        /**
         * Returns field.
         *
         * @return field
         */
        IStructuredBlobField<T> getField();

        /**
         * Returns identifier of start record.
         *
         * @return identifier of start record
         */
        long getStartId();

        /**
         * Returns identifier of end record.
         *
         * @return identifier of end record
         */
        long getEndId();

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
        T get();

        /**
         * Returns value of previous record.
         *
         * @return value of previous record or null if current record is first iteration record
         */
        T getPrevious();

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
     * @param <T> blob record type
     */
    interface IStructuredIterable<T> extends Iterable<T> {
        @Override
        IStructuredIterator<T> iterator();

        /**
         * Visits records of iterable.
         *
         * @param condition condition to select records, can be null if all records are selected
         * @param visitor   visitor to visit records
         */
        void visitRecords(ICondition<T> condition, IVisitor<T> visitor);
    }

    /**
     * Returns record indexer.
     *
     * @return record indexer
     */
    IRecordIndexer getRecordIndexer();

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
     * Returns current (last added) record identifier.
     *
     * @return current (last added) record identifier or 0 if blob is empty
     */
    long getCurrentId();

    /**
     * Returns current (last added) record.
     *
     * @return current (last added) record or null if blob is empty
     */
    T getCurrent();

    /**
     * Returns blob record by record identifier.
     *
     * @param id record identifier
     * @return blob record
     */
    T get(long id);

    /**
     * Returns iterable on all blob records.
     *
     * @return iterable on all blob records
     */
    IStructuredIterable<T> getRecords();

    /**
     * Returns iterable on blob records in specified range.
     *
     * @param startId identifier of first record in iterable
     * @param endId   identifier of last record or 0 if iterable is unbounded
     * @return iterable on blob records in specified range
     */
    IStructuredIterable<T> getRecords(long startId, long endId);

    /**
     * Returns reverse iterable on all blob records.
     *
     * @return reverse iterable on all blob records
     */
    IStructuredIterable<T> getReverseRecords();

    /**
     * Returns reverse iterable on blob records in specified range.
     *
     * @param startId identifier of first record in iterable
     * @param endId   identifier of last record or 0 if iterable is unbounded
     * @return iterable on blob records in specified range
     */
    IStructuredIterable<T> getReverseRecords(long startId, long endId);

    /**
     * Returns index.
     *
     * @param index numeric position in array of blob indexes
     * @return index
     */
    INodeIndex getIndex(int index);

    /**
     * Returns fulltext index.
     *
     * @return fulltext index or null if blob does not have fulltext index
     */
    INodeFullTextIndex getFullTextIndex();

    /**
     * Adds new blob record.
     *
     * @param record blob record
     * @return record identifier
     */
    long add(T record);

    /**
     * Sets new blob record (only for fixed records).
     *
     * @param record blob record
     * @param id     record identifier
     */
    void set(long id, T record);

    /**
     * Clears all blob contents.
     */
    void clear();
}
