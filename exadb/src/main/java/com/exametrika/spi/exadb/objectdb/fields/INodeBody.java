/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link INodeBody} represents a node body interface, which must be implemented by a node object in order to support body field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeBody extends INodeObject {
    /**
     * Sets body field. All modifications must be done using this field.
     *
     * @param field body field
     */
    void setField(IField field);

    /**
     * Migrates persistent data from specified body to this body.
     *
     * @param body node body to migrate data from
     */
    void migrate(INodeBody body);
}
