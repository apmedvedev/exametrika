/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;


/**
 * The {@link ISimpleField} represents a simple node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISimpleField extends IField {
    /**
     * Returns read region of field.
     *
     * @return read region of field
     */
    IRawReadRegion getReadRegion();

    /**
     * Returns write region of field.
     *
     * @return write region of field
     */
    IRawWriteRegion getWriteRegion();

    /**
     * Refreshes internal position of field's node in cache in order to prevent node unloading.
     */
    void refresh();
}
