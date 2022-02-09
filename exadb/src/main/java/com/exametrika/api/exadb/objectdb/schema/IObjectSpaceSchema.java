/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;


/**
 * The {@link IObjectSpaceSchema} represents a schema for object node space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectSpaceSchema extends INodeSpaceSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    ObjectSpaceSchemaConfiguration getConfiguration();

    /**
     * Returns parent.
     *
     * @return parent
     */
    @Override
    IDomainSchema getParent();

    /**
     * Returns space.
     *
     * @return space
     */
    IObjectSpace getSpace();
}
