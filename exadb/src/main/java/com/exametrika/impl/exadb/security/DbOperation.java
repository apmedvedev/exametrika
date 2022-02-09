/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.List;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DbOperation} is an implementation of database operation supporting operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DbOperation implements IOperation {
    private final Session session;
    private final IOperation operation;

    public DbOperation(Session session, IOperation operation) {
        Assert.notNull(session);
        Assert.notNull(operation);

        this.session = session;
        this.operation = operation;
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
    public void onCommitted() {
        operation.onCommitted();
    }

    @Override
    public void onRolledBack() {
        operation.onRolledBack();
    }

    @Override
    public void run(ITransaction transaction) {
        Assert.checkState(session.isOpened());
        Assert.checkState(session.getPrincipal().isAdministrator());

        session.activate(null);

        try {
            operation.run(transaction);
        } finally {
            session.deactivate();
        }
    }
}
