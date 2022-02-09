/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IBlob} represents a blob.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBlob {
    /**
     * Returns blob identifier.
     *
     * @return blob identifier
     */
    long getId();

    /**
     * Returns blob begin position.
     *
     * @return blob begin position
     */
    long getBeginPosition();

    /**
     * Returns blob end position.
     *
     * @return blob end position
     */
    long getEndPosition();

    /**
     * Returns blob store field, container of the blob.
     *
     * @return blob store field
     */
    IBlobStoreField getStore();

    /**
     * Creates blob serialization object. Blob serialization object is positioned at the end of the blob.
     *
     * @return blob serialization object
     */
    IBlobSerialization createSerialization();

    /**
     * Creates blob deserialization object. Blob deserialization object is positioned at the begin of the blob.
     *
     * @return blob deserialization object
     */
    IBlobDeserialization createDeserialization();

    /**
     * Deletes blob.
     */
    void delete();
}
