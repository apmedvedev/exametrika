/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import java.util.List;

import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;


/**
 * The {@link INodeSchema} represents a schema for node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeSchema extends ISchemaObject {
    String TYPE = "node";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    NodeSchemaConfiguration getConfiguration();

    /**
     * Returns index of schema in space schemas.
     *
     * @return index of schema in space schemas
     */
    int getIndex();

    /**
     * Returns space schema.
     *
     * @return space schema
     */
    @Override
    INodeSpaceSchema getParent();

    /**
     * Returns node's primary field schema.
     *
     * @return node's primary field schema or null if node does not have primary field
     */
    IFieldSchema getPrimaryField();

    /**
     * Returns node's version field schema.
     *
     * @return node's version field schema or null if node does not have version field
     */
    IFieldSchema getVersionField();

    /**
     * Returns node's body field schema.
     *
     * @return node's body field schema or null if node does not have body field
     */
    IFieldSchema getBodyField();

    /**
     * Returns list of field schemas.
     *
     * @return list of field schemas
     */
    List<IFieldSchema> getFields();

    /**
     * Finds field schema by name.
     *
     * @param <T>  field schema type
     * @param name name of field
     * @return field schema or null if field is not found
     */
    <T extends IFieldSchema> T findField(String name);

    /**
     * Finds field schema by alias.
     *
     * @param <T>   field schema type
     * @param alias alias of field
     * @return field schema or null if field is not found
     */
    <T extends IFieldSchema> T findFieldByAlias(String alias);

    /**
     * Returns fulltext document schema.
     *
     * @return fulltext document schema or null if nodes of this type are not indexed by full text search
     */
    IDocumentSchema getFullTextSchema();

    /**
     * Validates node consistency.
     *
     * @param node node to check
     * @throws RawRollbackException or any other exception if object is invalid
     */
    void validate(INode node);
}
