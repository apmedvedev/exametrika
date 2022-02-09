/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.rawdb.IRawTransaction;


/**
 * The {@link INode} represents a node. Nodes with equal identifiers are considered equals in particular space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INode {
    /**
     * Returns node identifier.
     *
     * @return node identifier
     */
    long getId();

    /**
     * Does node allow deletion?
     *
     * @return true if node allows deletion.
     */
    boolean allowDeletion();

    /**
     * Does node allow deletions in fields?
     *
     * @return true if node allows deletions in fields.
     */
    boolean allowFieldDeletion();

    /**
     * Is node read-only?
     *
     * @return true if node is read-only
     */
    boolean isReadOnly();

    /**
     * Is node deleted?
     *
     * @return true if node is deleted
     */
    boolean isDeleted();

    /**
     * Is node modified?
     *
     * @return true if node is modified
     */
    boolean isModified();

    /**
     * Notifies node that node is modified. Node object must call this method on all cached modifications.
     */
    void setModified();

    /**
     * Returns node schema.
     *
     * @return node schema
     */
    INodeSchema getSchema();

    /**
     * Returns node space.
     *
     * @return node space
     */
    INodeSpace getSpace();

    /**
     * Returns current raw transaction.
     *
     * @return current raw transaction
     */
    IRawTransaction getRawTransaction();

    /**
     * Returns current transaction.
     *
     * @return current transaction
     */
    ITransaction getTransaction();

    /**
     * Returns total cache size of node, including node object and node fields.
     *
     * @return total cache size of node
     */
    int getCacheSize();

    /**
     * Returns node object which implements typed access to node.
     *
     * @param <T> object type
     * @return node object
     */
    <T> T getObject();

    /**
     * Returns field count.
     *
     * @return field count
     */
    int getFieldCount();

    /**
     * Returns field.
     *
     * @param <T>   field type
     * @param index field index
     * @return field
     */
    <T> T getField(int index);

    /**
     * Returns field.
     *
     * @param <T>    field type
     * @param schema field schema
     * @return field
     */
    <T> T getField(IFieldSchema schema);

    /**
     * Deletes node.
     */
    void delete();

    /**
     * Updates current cache size of node.
     *
     * @param delta positive or negative delta value
     */
    void updateCacheSize(int delta);


    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
