/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;


/**
 * The {@link IDatabaseContext} represents a database context.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDatabaseContext {
    /**
     * Returns database configuration.
     *
     * @return database configuration
     */
    DatabaseConfiguration getConfiguration();

    /**
     * Returns database.
     *
     * @return database
     */
    IDatabase getDatabase();

    /**
     * Returns raw database.
     *
     * @return raw database
     */
    IRawDatabase getRawDatabase();

    /**
     * Returns database compartment.
     *
     * @return database compartment
     */
    ICompartment getCompartment();

    /**
     * Returns schema space.
     *
     * @return schema space
     */
    ISchemaSpace getSchemaSpace();

    /**
     * Returns transaction provider.
     *
     * @return transaction provider
     */
    ITransactionProvider getTransactionProvider();

    /**
     * Returns cache control.
     *
     * @return cache control
     */
    ICacheControl getCacheControl();

    /**
     * Returns extension space.
     *
     * @return extension space
     */
    IExtensionSpace getExtensionSpace();

    /**
     * Returns time service.
     *
     * @return time service
     */
    ITimeService getTimeService();

    /**
     * Returns cache categorization strategy.
     *
     * @return cache categorization strategy
     */
    ICacheCategorizationStrategy getCacheCategorizationStrategy();

    /**
     * Returns resource allocator.
     *
     * @return resource allocator
     */
    IResourceAllocator getResourceAllocator();

    /**
     * Finds extension by name.
     *
     * @param name extension name
     * @return extension or null if extension is not found
     */
    <T extends IDatabaseExtension> T findExtension(String name);

    /**
     * Finds public extension by name.
     *
     * @param name public extension name
     * @return extension or null if extension is not found
     */
    <T> T findPublicExtension(String name);

    /**
     * Finds public transaction extension by name.
     *
     * @param name public transaction extension name
     * @return extension or null if extension is not found
     */
    <T> T findTransactionExtension(String name);
}
