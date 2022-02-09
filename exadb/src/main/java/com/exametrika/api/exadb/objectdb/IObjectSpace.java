/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;


/**
 * The {@link IObjectSpace} represents an object space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectSpace extends INodeSpace {
    /**
     * Returns schema of space.
     *
     * @return space schema
     */
    IObjectSpaceSchema getSchema();

    /**
     * Returns root node.
     *
     * @return root node or null if root node does not defined for this space
     */
    <T> T getRootNode();

    /**
     * Finds node by node identifier.
     *
     * @param <T> node type
     * @param id  node identifier
     * @return found node
     */
    <T> T findNodeById(long id);

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
     * Returns fulltext index.
     *
     * @return fulltext index or null if space does not have fulltext index
     */
    INodeFullTextIndex getFullTextIndex();

    /**
     * Creates new node (if it does not exist) or returns existing node by primary key. Current transaction must be write transaction in order to add new node.
     *
     * @param <T>    node type
     * @param key    primary key or null if node does not have primary key
     * @param schema node schema
     * @param args   additional node creation arguments
     * @return node
     */
    <T> T findOrCreateNode(Object key, INodeSchema schema, Object... args);

    /**
     * Is node with specified key contained in the space. Must be used only for nodes with primary keys.
     *
     * @param key    primary key
     * @param schema node schema
     * @return if true node is contained in the space
     */
    boolean containsNode(Object key, INodeSchema schema);

    /**
     * Finds existing node by node location. Must be used only for nodes with primary keys.
     *
     * @param <T>    node type
     * @param key    primary key
     * @param schema node schema
     * @return node or null if node is not found
     */
    <T> T findNode(Object key, INodeSchema schema);

    /**
     * Creates new node. Current transaction must be write transaction in order to add new node.
     *
     * @param <T>    node type
     * @param key    primary key or null if node does not have primary key
     * @param schema node schema
     * @param args   additional node creation arguments
     * @return node
     */
    <T> T createNode(Object key, INodeSchema schema, Object... args);

    /**
     * Returns iterator for all nodes of specified type starting from last added.
     *
     * @param <T>    node type
     * @param schema node schema
     * @return iterator for all nodes in reverse creation order
     */
    <T> Iterable<T> getNodes(INodeSchema schema);

    /**
     * Returns iterator for all nodes starting from last added.
     *
     * @param <T> node type
     * @return iterator for all nodes in reverse creation order
     */
    <T> Iterable<T> getNodes();
}
