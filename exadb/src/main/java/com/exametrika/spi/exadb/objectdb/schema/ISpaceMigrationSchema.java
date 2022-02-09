/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;


/**
 * The {@link ISpaceMigrationSchema} represents a schema of migration old space to new one.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISpaceMigrationSchema {
    /**
     * Returns old space schema.
     *
     * @return old space schema
     */
    IObjectSpaceSchema getOldSchema();

    /**
     * Returns new space schema.
     *
     * @return new space schema
     */
    IObjectSpaceSchema getNewSchema();

    /**
     * Returns root node migration schema.
     *
     * @return root node migration schema or null if root node type has been changed in new space schema.
     */
    INodeMigrationSchema getRoot();

    /**
     * List of migration schemas of those nodes in old space that required migration into the new one.
     *
     * @return list of node migration schemas
     */
    List<INodeMigrationSchema> getNodes();
}
