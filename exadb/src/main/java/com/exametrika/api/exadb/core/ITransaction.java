/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.List;

import com.exametrika.api.exadb.core.schema.IDatabaseSchema;


/**
 * The {@link ITransaction} represents a transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITransaction {
    /**
     * Is transaction read-only?
     *
     * @return true if transaction read-only
     */
    boolean isReadOnly();

    /**
     * Returns transaction options.
     *
     * @return transaction options
     */
    int getOptions();

    /**
     * Returns active transaction operation.
     *
     * @return active transaction operation
     */
    <T> T getOperation();

    /**
     * Returns transaction start time.
     *
     * @return transaction start time
     */
    long getTime();

    /**
     * Returns database.
     *
     * @return database
     */
    IDatabase getDatabase();

    /**
     * Returns current database schema.
     *
     * @return current database schema or null if schema is not set
     */
    IDatabaseSchema getCurrentSchema();

    /**
     * Returns all database schemas in chronological order.
     *
     * @return all database schemas in chronological order
     */
    List<IDatabaseSchema> getSchemas();

    /**
     * Finds nearest database schema whose creation time is smaller than specified time.
     *
     * @param time time
     * @return found database schema or null if database schema is not found
     */
    IDatabaseSchema findSchema(long time);

    /**
     * Finds public transaction extension by name.
     *
     * @param name public transaction extension name
     * @return extension or null if extension is not found
     */
    <T> T findExtension(String name);

    /**
     * Finds domain service by fully qualified name.
     *
     * @param name fully qualified name of domain service
     * @return domain service or null if domain service is not found
     */
    <T> T findDomainService(String name);
}
