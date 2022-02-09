/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IRecordIndexer} represents an indexer of records of structured blob field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRecordIndexer {
    /**
     * Adds record to indexes.
     *
     * @param record record
     * @param id     record identifier
     */
    void addRecord(Object record, long id);

    /**
     * Removes record from indexes.
     *
     * @param record record
     */
    void removeRecord(Object record);

    /**
     * Reindex record in full text index.
     *
     * @param record record to reindex
     * @param id     record identifer
     */
    void reindex(Object record, long id);
}
