/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.List;

import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.rawdb.RawRollbackException;


/**
 * The {@link IBatchOperation} represents a batch (long running) operation executed in transaction cooperatively
 * with other (ordinary) transactions. Batch operation is executed in several steps. On first step operation validation
 * is occured, only on this step transaction can be rolled back. On other steps operation is running by time slices,
 * allowing other ordinary transactions to execute. If process failure is occured, transaction is restarted from last
 * successfully executed step. All exceptions during run steps are considered as successfull operation completion
 * because transaction can not be rolled back on run steps.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBatchOperation {
    /**
     * Returns operation transaction options as defined in {@link IOperation}.
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
     * Returns locks of batch operation. Batch locks can be changed dynamically during course of execution of batch operation.
     * Using this method batch operation can dynamically updates list of required
     * locks allowing or denying cooperative execution of other transactions.
     *
     * @return locks of batch operation or empty list if batch operation does not require locks
     */
    List<RawBatchLock> getLocks();

    /**
     * Checks batch preconditions. Check operation must be small enough, because it blocks all other transactions.
     *
     * @param transaction transaction
     * @throws RawRollbackException (or any other exception) if batch transaction is rolled back
     */
    void validate(ITransaction transaction);

    /**
     * Runs operation in transaction.
     *
     * @param transaction  enclosing transaction
     * @param batchControl batch control
     * @return true if operation has been completed, false if additional operation steps are required
     * @throws RawRollbackException (or any other exception) operation is considered as successfully completed
     */
    boolean run(ITransaction transaction, IBatchControl batchControl);

    /**
     * Called when last run step of write batch transaction is successfully committed.
     */
    void onCommitted();

    /**
     * Called when validation step of write batch transaction failed and transaction is rolled back.
     */
    void onRolledBack();
}
