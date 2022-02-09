/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import java.util.List;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.spi.exadb.core.ICacheControl;

/**
 * The {@link DbOperation} is a db operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DbOperation implements IRawOperation {
    private final TransactionManager transactionManager;
    private final IOperation operation;
    private final SchemaSpace schemaSpace;

    public DbOperation(TransactionManager transactionManager, IOperation operation, SchemaSpace schemaSpace) {
        Assert.notNull(transactionManager);
        Assert.notNull(operation);
        Assert.notNull(schemaSpace);

        this.transactionManager = transactionManager;
        this.operation = operation;
        this.schemaSpace = schemaSpace;
    }

    @Override
    public int getOptions() {
        return operation.getOptions();
    }

    @Override
    public int getSize() {
        return operation.getSize();
    }

    @Override
    public List<String> getBatchLockPredicates() {
        return operation.getBatchLockPredicates();
    }

    @Override
    public boolean isCompleted() {
        return true;
    }

    @Override
    public void onBeforeStarted(IRawTransaction transaction) {
        Spaces.bindSystemFiles(schemaSpace.getDatabase().getContext(), transaction);

        Transaction exaTransaction = new Transaction(transaction, operation.getOptions(),
                schemaSpace.getDatabase(), schemaSpace.getExtensionManager(), schemaSpace.getDomainServiceManager(), operation);

        transactionManager.setTransaction(exaTransaction);

        getCacheControl().onTransactionStarted();
    }

    @Override
    public void run(IRawTransaction transaction) {
        operation.run(transactionManager.getTransaction());
    }

    @Override
    public void validate() {
        getCacheControl().validate();
    }

    @Override
    public void onBeforeCommitted() {
        getCacheControl().onTransactionCommitted();
    }

    @Override
    public void onCommitted() {
        operation.onCommitted();

        transactionManager.setTransaction(null);
    }

    @Override
    public boolean onBeforeRolledBack() {
        return getCacheControl().onBeforeTransactionRolledBack();
    }

    @Override
    public void onRolledBack(boolean clearCache) {
        getCacheControl().onTransactionRolledBack();
        if (clearCache)
            getCacheControl().clear(true);

        operation.onRolledBack();

        transactionManager.setTransaction(null);
    }

    private ICacheControl getCacheControl() {
        return schemaSpace.getDatabase().getContext().getCacheControl();
    }
}