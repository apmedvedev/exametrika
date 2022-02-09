/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.schema;

import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link ISpaceSchema} represents a schema for space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISpaceSchema extends ISchemaObject {
    String TYPE = "space";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    SpaceSchemaConfiguration getConfiguration();

    /**
     * Returns version of schema space.
     *
     * @return version of schema space
     */
    int getVersion();

    /**
     * Returns parent.
     *
     * @return parent
     */
    @Override
    ISchemaObject getParent();
}
