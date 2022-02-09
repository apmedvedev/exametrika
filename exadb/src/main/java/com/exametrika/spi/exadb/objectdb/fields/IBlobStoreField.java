/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IBlobStoreField} represents a blob store field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBlobStoreField extends IField {
    /**
     * Returns free space of this blob storage.
     *
     * @return free space of this blob storage
     */
    long getFreeSpace();

    /**
     * Creates a new blob.
     *
     * @return a new blob
     */
    IBlob createBlob();

    /**
     * Opens blob.
     *
     * @param blobId blob identifier
     * @return blob
     */
    IBlob openBlob(long blobId);
}
