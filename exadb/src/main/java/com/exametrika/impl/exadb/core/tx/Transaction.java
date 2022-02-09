/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import java.util.List;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseExtensionManager;
import com.exametrika.impl.exadb.core.DomainServiceManager;

/**
 * The {@link Transaction} is a transaction.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class Transaction implements com.exametrika.api.exadb.core.ITransaction {
    private final com.exametrika.common.rawdb.IRawTransaction transaction;
    private final int options;
    private final Database database;
    private final DatabaseExtensionManager extensionManager;
    private final DomainServiceManager domainServiceManager;
    private final Object operation;

    public Transaction(com.exametrika.common.rawdb.IRawTransaction transaction, int options,
                       Database database, DatabaseExtensionManager extensionManager, DomainServiceManager domainServiceManager,
                       Object operation) {
        Assert.notNull(transaction);
        Assert.notNull(database);
        Assert.notNull(extensionManager);
        Assert.notNull(domainServiceManager);
        Assert.notNull(operation);

        this.transaction = transaction;
        this.options = options;
        this.database = database;
        this.extensionManager = extensionManager;
        this.domainServiceManager = domainServiceManager;
        this.operation = operation;
    }

    public IRawTransaction getTransaction() {
        return transaction;
    }

    @Override
    public boolean isReadOnly() {
        return transaction.isReadOnly();
    }

    @Override
    public int getOptions() {
        return options;
    }

    @Override
    public IDatabase getDatabase() {
        return database;
    }

    @Override
    public <T> T getOperation() {
        return (T) operation;
    }

    @Override
    public long getTime() {
        return transaction.getTime();
    }

    @Override
    public IDatabaseSchema getCurrentSchema() {
        return database.getContext().getSchemaSpace().getCurrentSchema();
    }

    @Override
    public List<IDatabaseSchema> getSchemas() {
        return database.getContext().getSchemaSpace().getSchemas();
    }

    @Override
    public IDatabaseSchema findSchema(long time) {
        return database.getContext().getSchemaSpace().findSchema(time);
    }

    @Override
    public <T> T findExtension(String name) {
        return extensionManager.findTransactionExtension(name);
    }

    @Override
    public <T> T findDomainService(String name) {
        return domainServiceManager.findDomainService(name);
    }
}