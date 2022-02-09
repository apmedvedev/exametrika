/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.List;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.IOperationWrapper;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.security.ISecuredOperation;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DbSecuredOperation} is an implementation of database operation supporting secured operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DbSecuredOperation implements IOperation, IOperationWrapper {
    private final Session session;
    private final ISecuredOperation operation;

    public DbSecuredOperation(Session session, ISecuredOperation operation) {
        Assert.notNull(session);
        Assert.notNull(operation);

        this.session = session;
        this.operation = operation;
    }

    @Override
    public ISecuredOperation getOperation() {
        return operation;
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
        session.deactivate();
    }

    @Override
    public void onRolledBack() {
        operation.onRolledBack();
        session.deactivate();
    }

    @Override
    public void run(ITransaction transaction) {
        Assert.checkState(session.isOpened());
        SecuredTransaction securedTransaction = new SecuredTransaction(session, transaction);
        session.activate(securedTransaction);

        operation.run(securedTransaction);
    }
}
