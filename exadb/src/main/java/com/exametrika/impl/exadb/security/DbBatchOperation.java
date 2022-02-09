/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.io.Serializable;
import java.util.List;

import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DbBatchOperation} is an implementation of database operation supporting batch operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DbBatchOperation implements IBatchOperation, Serializable {
    private final String userName;
    private final IBatchOperation operation;
    private transient Session session;

    public DbBatchOperation(Session session, IBatchOperation operation) {
        Assert.notNull(session);
        Assert.notNull(operation);

        this.session = session;
        this.userName = session.getPrincipal().getUser().getName();
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
    public List<RawBatchLock> getLocks() {
        return operation.getLocks();
    }

    @Override
    public void validate(ITransaction transaction) {
        operation.validate(transaction);
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
    public boolean run(ITransaction transaction, IBatchControl batchControl) {
        ensureSession(transaction);
        Assert.checkState(session.isOpened());
        Assert.checkState(session.getPrincipal().isAdministrator());

        session.activate(null);

        try {
            return operation.run(transaction, batchControl);
        } finally {
            session.deactivate();
        }
    }

    private void ensureSession(ITransaction transaction) {
        if (session != null)
            return;

        ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
        session = (Session) securityService.login(userName);
    }
}
