/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox;

import com.exametrika.common.io.IDataDeserialization;


/**
 * The {@link IIndexValueDeserialization} represents a helper object to deserialize index values.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexValueDeserialization extends IDataDeserialization {
    /**
     * Returns identifier of area of current serialization position.
     *
     * @return identifier of area of current serialization position
     */
    long getAreaId();

    /**
     * Returns offset of area of current serialization position.
     *
     * @return offset of area of current serialization position
     */
    int getAreaOffset();

    /**
     * Returns identifier of last area.
     *
     * @return identifier of last area
     */
    long getLastAreaId();

    /**
     * Does stream contains available data to read.
     *
     * @param readSize size of data to read must be less than area size
     * @return true if stream has available data to read
     */
    boolean hasNext(int readSize);

    /**
     * Sets current serialization position.
     *
     * @param areaId     identifier of area of new serialization position
     * @param areaOffset offset of area of new serialization position
     */
    void setPosition(long areaId, int areaOffset);
}
