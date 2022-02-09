/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.schema.INodeSchema;


/**
 * The {@link INodeMigrationSchema} represents a schema of migration old node to new one.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeMigrationSchema {
    /**
     * Returns space migration schema.
     *
     * @return space migration schema
     */
    ISpaceMigrationSchema getSpace();

    /**
     * Return node schema in old space.
     *
     * @return node schema in old space
     */
    INodeSchema getOldSchema();

    /**
     * Return node schema in new space.
     *
     * @return node schema in new space
     */
    INodeSchema getNewSchema();

    /**
     * Returns migration schema for primary field.
     *
     * @return migration schema for primary field or null if node does not contain primary field
     */
    IFieldMigrationSchema getPrimaryField();

    /**
     * List of migration schemas of those fields in old node that required migration into the new one.
     *
     * @return list of field migration schemas
     */
    List<IFieldMigrationSchema> getFields();
}
