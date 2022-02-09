/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IFieldObject} represents a typed field object.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFieldObject {
    /**
     * Called when new node is created.
     *
     * @param primaryKey primary key of node or null if node does not have primary key
     * @param initalizer field initializer or null if initializer is not used
     */
    void onCreated(Object primaryKey, Object initalizer);

    /**
     * Called after all node fields are created.
     *
     * @param primaryKey primary key of node or null if node does not have primary key
     * @param initalizer field initializer or null if initializer is not used
     */
    void onAfterCreated(Object primaryKey, Object initalizer);

    /**
     * Called when existing node is opened.
     */
    void onOpened();

    /**
     * Called when node is deleted.
     */
    void onDeleted();

    /**
     * Called when node is unloaded from memory.
     */
    void onUnloaded();

    /**
     * Flushes all cached modifications (if any) to field.
     */
    void flush();
}
