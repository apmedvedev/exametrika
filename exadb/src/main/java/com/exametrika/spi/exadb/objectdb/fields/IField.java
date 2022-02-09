/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;


/**
 * The {@link IField} represents a node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IField {
    /**
     * Is field read-only?
     *
     * @return true if field is read-only
     */
    boolean isReadOnly();

    /**
     * Does field allow deletion?
     *
     * @return true if field allows deletion.
     */
    boolean allowDeletion();

    /**
     * Returns field schema.
     *
     * @return field schema
     */
    IFieldSchema getSchema();

    /**
     * Returns field node.
     *
     * @return field node
     */
    INode getNode();

    /**
     * Returns field value.
     *
     * @return field value
     */
    <T> T get();

    /**
     * Returns field object which implements typed access to field.
     *
     * @param <T> object type
     * @return field object
     */
    <T> T getObject();

    /**
     * Notifies field that field is modified. Field object must call this method on all cached modifications.
     */
    void setModified();
}
