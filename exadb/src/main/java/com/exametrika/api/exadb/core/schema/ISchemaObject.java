/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.schema;

import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link ISchemaObject} represents a base schema object.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchemaObject {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    SchemaConfiguration getConfiguration();

    /**
     * Returns unique identifier
     *
     * @return unique identifier
     */
    String getId();

    /**
     * Returns fully qualified name.
     *
     * @return fully qualified name
     */
    String getQualifiedName();

    /**
     * Returns fully qualified alias.
     *
     * @return fully qualified alias
     */
    String getQualifiedAlias();

    /**
     * Returns type.
     *
     * @return type
     */
    String getType();

    /**
     * Returns root schema object - database schema.
     *
     * @return root schema object - database schema
     */
    IDatabaseSchema getRoot();

    /**
     * Returns parent.
     *
     * @return parent or null if this schema object represents root database schema
     */
    <T extends ISchemaObject> T getParent();

    /**
     * Returns all children.
     *
     * @return all children
     */
    Iterable<ISchemaObject> getChildren();

    /**
     * Returns all children of specified type.
     *
     * @param type children type
     * @return all children of specified type
     */
    Iterable<ISchemaObject> getChildren(String type);

    /**
     * Finds child by name and type.
     *
     * @param type type
     * @param name name
     * @return child or null if child is not found
     */
    <T extends ISchemaObject> T findChild(String type, String name);

    /**
     * Finds child by alias and type.
     *
     * @param type  type
     * @param alias alias
     * @return child or null if child is not found
     */
    <T extends ISchemaObject> T findChildByAlias(String type, String alias);
}
