/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.common.io.IDataSerialization;


/**
 * The {@link IBlobSerialization} represents a helper object to serialize values into blob.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBlobSerialization extends IBlobDeserialization, IDataSerialization {
    /**
     * Updates end serialized position in blob.
     */
    void updateEndPosition();

    /**
     * Removes all unused blob pages starting from current position to the rest of the blob. Current position becomes end position of the blob.
     */
    void removeRest();
}
