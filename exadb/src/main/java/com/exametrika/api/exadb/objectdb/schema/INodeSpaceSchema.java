/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import java.util.List;

import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSpaceSchemaConfiguration;


/**
 * The {@link INodeSpaceSchema} represents a schema for node space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeSpaceSchema extends ISpaceSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    NodeSpaceSchemaConfiguration getConfiguration();

    /**
     * Returns schema of root node.
     *
     * @return schema of root node or null if root node is not set
     */
    INodeSchema getRootNode();

    /**
     * Returns list of node schemas.
     *
     * @return list of node schemas
     */
    List<INodeSchema> getNodes();

    /**
     * Finds node schema by name.
     *
     * @param <T>  node schema type
     * @param name name of node
     * @return node schema or null if node is not found
     */
    <T extends INodeSchema> T findNode(String name);

    /**
     * Finds node schema by alias.
     *
     * @param <T>   node schema type
     * @param alias alias of node
     * @return node schema or null if node is not found
     */
    <T extends INodeSchema> T findNodeByAlias(String alias);
}
