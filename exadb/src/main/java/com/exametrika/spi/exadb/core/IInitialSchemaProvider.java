/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;


/**
 * The {@link IInitialSchemaProvider} represents a provider of initial schema of database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInitialSchemaProvider {
    /**
     * Returns initial schema of database.
     *
     * @return initial schema of database
     */
    ModularDatabaseSchemaConfiguration getInitialSchema();
}
