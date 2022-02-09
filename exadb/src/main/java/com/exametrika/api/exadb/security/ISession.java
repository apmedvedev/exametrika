/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import java.io.Closeable;

import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaOperation;


/**
 * The {@link ISession} represents an security session.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISession extends Closeable {
    /**
     * Is session opened?
     *
     * @return true if session is opened
     */
    boolean isOpened();

    /**
     * Performs data manipulation operation in transaction asynchronously. Session user must have administrative permissions
     * to perform this operation.
     *
     * @param operation operation to execute
     */
    void transaction(IOperation operation);

    /**
     * Performs data manipulation operation in transaction synchronously. Can not be called from main transaction thread.
     * Session user must have administrative permissions to perform this operation.
     *
     * @param operation operation to execute
     */
    void transactionSync(IOperation operation);

    /**
     * Performs batch data manipulation operation in transaction asynchronously. Session user must have administrative permissions
     * to perform this operation.
     *
     * @param operation operation to execute
     */
    void transaction(IBatchOperation operation);

    /**
     * Performs batch data manipulation operation in transaction synchronously. Can not be called from main transaction thread.
     * Session user must have administrative permissions to perform this operation.
     *
     * @param operation operation to execute
     */
    void transactionSync(IBatchOperation operation);

    /**
     * Performs schema change operation in transaction asynchronously. Session user must have administrative permissions
     * to perform this operation.
     *
     * @param operation operation to execute
     */
    void transaction(ISchemaOperation operation);

    /**
     * Performs schema change operation in transaction synchronously. Can not be called from main transaction thread.
     * Session user must have administrative permissions to perform this operation.
     *
     * @param operation operation to execute
     */
    void transactionSync(ISchemaOperation operation);

    /**
     * Performs data manipulation operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(ISecuredOperation operation);

    /**
     * Performs data manipulation operation in transaction synchronously.
     *
     * @param operation operation to execute
     */
    void transactionSync(ISecuredOperation operation);

    /**
     * Closes session.
     */
    @Override
    void close();
}
