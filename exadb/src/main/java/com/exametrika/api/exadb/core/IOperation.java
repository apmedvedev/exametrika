/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.List;

import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.RawRollbackException;


/**
 * The {@link IOperation} represents an operation executed in transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IOperation {
    /**
     * Is transaction read-only or read-write transaction?
     */
    int READ_ONLY = IRawOperation.READ_ONLY;

    /**
     * This option requires transaction to make all transaction changes durable before commit completes.
     */
    int DURABLE = IRawOperation.DURABLE;

    /**
     * This option requires transaction to flush all pending changes to disk before transaction start and after commit.
     */
    int FLUSH = IRawOperation.FLUSH;

    /**
     * This option delays flushing of transaction changes.
     */
    int DELAYED_FLUSH = 0x100;

    /**
     * This option disables unload of data nodes during current transaction.
     */
    int DISABLE_NODES_UNLOAD = 0x200;

    /**
     * Returns operation transaction options.
     *
     * @return operation transaction options
     */
    int getOptions();

    /**
     * Returns estimated operation size.
     *
     * @return estimated operation size
     */
    int getSize();

    /**
     * Returns batch lock predicates. Lock predicate allows to control isolation between ordinary transactions and batch transaction.
     * If ordinary transaction and batch transaction have intersecting lock perdicates
     * ordinary transaction is blocked until batch transaction has been completed. Two lock predicates are block each other if they
     * are equal or one lock predicate is prefix of another lock predicate.
     *
     * @return batch lock predicates or null if lock of entire database is requested
     */
    List<String> getBatchLockPredicates();

    /**
     * Runs operation in transaction.
     *
     * @param transaction enclosing transaction
     * @throws RawRollbackException (or any other exception) if transaction is rolled back
     */
    void run(ITransaction transaction);

    /**
     * Called when write transaction is successfully committed.
     */
    void onCommitted();

    /**
     * Called when write transaction is rolled back.
     */
    void onRolledBack();
}
