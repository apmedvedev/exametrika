/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link IObjectNodeSchema} represents a schema for object node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectNodeSchema extends INodeSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    ObjectNodeSchemaConfiguration getConfiguration();
}
