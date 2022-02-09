/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.io.Closeable;
import java.util.List;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;


/**
 * The {@link IDatabase} represents a exa database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDatabase extends Closeable {
    String NAME = "exadb";

    /**
     * Is database opened?
     *
     * @return true if database is opened
     */
    boolean isOpened();

    /**
     * Is database closed?
     *
     * @return true if database is closed
     */
    boolean isClosed();

    /**
     * Returns database configuration.
     *
     * @return database configuration
     */
    DatabaseConfiguration getConfiguration();

    /**
     * Sets database configuration.
     *
     * @param configuration database configuration
     */
    void setConfiguration(DatabaseConfiguration configuration);

    /**
     * Finds parameter by name.
     *
     * @param name parameter name
     * @return parameter value or null if parameter is not found
     */
    <T> T findParameter(String name);

    /**
     * Returns manager of maintanance operations.
     *
     * @return manager of maintanance operations
     */
    IOperationManager getOperations();

    /**
     * Finds public extension by name.
     *
     * @param name public extension name
     * @return extension or null if extension is not found
     */
    <T> T findExtension(String name);

    /**
     * Opens database.
     */
    void open();

    /**
     * Closes database.
     */
    @Override
    void close();

    /**
     * Flushes database.
     */
    void flush();

    /**
     * Clears all internal page, file and other caches. All file binding information is also cleared.
     */
    void clearCaches();

    /**
     * Performs data manipulation operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(IOperation operation);

    /**
     * Performs list of data manipulation operations in transactions asynchronously. Each operation is performed in its own transaction.
     *
     * @param operations operations to execute
     */
    void transaction(List<IOperation> operations);

    /**
     * Performs data manipulation operation in transaction synchronously. Can not be called from main transaction thread.
     *
     * @param operation operation to execute
     */
    void transactionSync(IOperation operation);

    /**
     * Performs batch data manipulation operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(IBatchOperation operation);

    /**
     * Performs batch data manipulation operation in transaction synchronously. Can not be called from main transaction thread.
     *
     * @param operation operation to execute
     */
    void transactionSync(IBatchOperation operation);

    /**
     * Performs schema change operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(ISchemaOperation operation);

    /**
     * Performs schema change operation in transaction synchronously. Can not be called from main transaction thread.
     *
     * @param operation operation to execute
     */
    void transactionSync(ISchemaOperation operation);
}
