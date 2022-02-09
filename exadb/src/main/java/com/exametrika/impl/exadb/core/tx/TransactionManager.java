/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link TransactionManager} is a transaction manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TransactionManager implements ITransactionProvider {
    private final IRawDatabase database;
    private Transaction transaction;
    private IRawTransaction rawTransaction;
    private SchemaSpace schemaSpace;

    public TransactionManager(IRawDatabase database) {
        Assert.notNull(database);

        this.database = database;
    }

    public IRawDatabase getDatabase() {
        return database;
    }

    public SchemaSpace getSchemaSpace() {
        return schemaSpace;
    }

    public void setSchemaSpace(SchemaSpace schemaSpace) {
        Assert.notNull(schemaSpace);
        Assert.checkState(this.schemaSpace == null);

        this.schemaSpace = schemaSpace;
    }

    @Override
    public IRawTransaction getRawTransaction() {
        return rawTransaction;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transaction = transaction;
            this.rawTransaction = transaction.getTransaction();
        } else {
            this.transaction = null;
            this.rawTransaction = null;
        }
    }

    public void transaction(IOperation operation) {
        database.transaction(new DbOperation(this, operation, schemaSpace));
    }

    public void transaction(List<IOperation> operations) {
        List<IRawOperation> dbOperations = new ArrayList<IRawOperation>(operations.size());
        for (IOperation operation : operations)
            dbOperations.add(new DbOperation(this, operation, schemaSpace));

        database.transaction(dbOperations);
    }

    public void transactionSync(IOperation operation) {
        database.transactionSync(new DbOperation(this, operation, schemaSpace));
    }

    public void transaction(IBatchOperation operation) {
        database.transaction(new DbBatchOperation(operation, true, null));
    }

    public void transactionSync(IBatchOperation operation) {
        database.transactionSync(new DbBatchOperation(operation, true, null));
    }

    public void transaction(DbBatchOperation operation) {
        database.transaction(operation);
    }

    public void transactionSync(DbBatchOperation operation) {
        database.transactionSync(operation);
    }
}
