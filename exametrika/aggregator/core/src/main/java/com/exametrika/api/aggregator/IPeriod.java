/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;


/**
 * The {@link IPeriod} represents a period.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriod {
    /**
     * Returns period type name.
     *
     * @return period type name
     */
    String getType();

    /**
     * Returns period's space.
     *
     * @return space of period
     */
    IPeriodSpace getSpace();

    /**
     * Returns period start time.
     *
     * @return period start time
     */
    long getStartTime();

    /**
     * Returns period end time.
     *
     * @return period end time or 0 if period is current (is not ended yet)
     */
    long getEndTime();

    /**
     * Returns root node.
     *
     * @return root node or null if root node does not defined for space of this period
     */
    <T> T getRootNode();

    /**
     * Returns index for specified indexed field.
     *
     * @param field indexed field
     * @return node index
     */
    <T extends INodeIndex> T getIndex(IFieldSchema field);

    /**
     * Returns index for specified index name.
     *
     * @param indexName index name
     * @return node index or null if index is not found
     */
    <T extends INodeIndex> T findIndex(String indexName);

    /**
     * Finds node by the same primary key as in specified node.
     *
     * @param <T>  node type
     * @param node node
     * @return found node or null if node is not found
     */
    <T> T findNode(T node);

    /**
     * Finds node by node identifier.
     *
     * @param <T> node type
     * @param id  node identifier
     * @return found node
     */
    <T> T findNodeById(long id);

    /**
     * Creates new node (if it does not exist) or returns existing node by node location. Current transaction must be write transaction
     * and period must be current in order to add new node.
     *
     * @param <T>      node type
     * @param location node location
     * @param schema   node schema
     * @param args     additional node creation arguments
     * @return node
     */
    <T> T findOrCreateNode(Location location, INodeSchema schema, Object... args);

    /**
     * Is node with specified location contained in the period.
     *
     * @param location node location
     * @param schema   node schema
     * @return if true node is contained in the space
     */
    boolean containsNode(Location location, INodeSchema schema);

    /**
     * Finds existing node by node location.
     *
     * @param <T>      node type
     * @param location node location
     * @param schema   node schema
     * @return node or null if node is not found
     */
    <T> T findNode(Location location, INodeSchema schema);

    /**
     * Creates new node. Current transaction must be write transaction and period must be current in order to add new node.
     *
     * @param <T>      node type
     * @param location node location
     * @param schema   node schema
     * @param args     additional node creation arguments
     * @return node
     */
    <T> T createNode(Location location, INodeSchema schema, Object... args);

    /**
     * Returns iterator for all period nodes starting from last added.
     *
     * @param <T> node type
     * @return iterator for all nodes in reverse creation order
     */
    <T> Iterable<T> getNodes();
}
