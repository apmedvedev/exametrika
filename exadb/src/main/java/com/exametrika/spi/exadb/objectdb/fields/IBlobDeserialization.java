/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.common.io.IDataDeserialization;


/**
 * The {@link IBlobDeserialization} represents a helper object to deserialize values from blob.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBlobDeserialization extends IDataDeserialization {
    /**
     * Returns blob;
     *
     * @return blob
     */
    IBlob getBlob();

    /**
     * Returns current serialization position
     *
     * @return current serialization position
     */
    long getPosition();

    /**
     * Returns begin serialization position.
     *
     * @return end serialization position
     */
    long getBeginPosition();

    /**
     * Returns end serialization position. End position follows last serialized position of the blob.
     *
     * @return end serialization position
     */
    long getEndPosition();

    /**
     * Sets current serialization position.
     *
     * @param position new serialization position
     */
    void setPosition(long position);
}
