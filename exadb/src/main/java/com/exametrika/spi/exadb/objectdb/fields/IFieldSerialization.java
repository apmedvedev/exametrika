/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.common.io.IDataSerialization;


/**
 * The {@link IFieldSerialization} represents a helper object to serialize values into field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFieldSerialization extends IFieldDeserialization, IDataSerialization {
    /**
     * Removes all unused field areas starting from current position to the rest of the field.
     */
    void removeRest();

    /**
     * Increments usage count of current field area.
     */
    void incrementCurrentAreaUsageCount();

    /**
     * Decrements usage count of current field area. If usage count of field area is set to 0, area is removed
     * from the field. Serialization position is set to the next area (if any).
     */
    void decrementCurrentAreaUsageCount();

    /**
     * Creates cloned copy of current object.
     *
     * @return cloned copy of current object
     */
    IFieldSerialization clone();
}
