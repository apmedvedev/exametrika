/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.concurrent.Callable;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.security.ISecuredTransaction;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.security.IPrincipal;


/**
 * The {@link SecuredTransaction} is a secured transaction.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecuredTransaction implements ISecuredTransaction {
    private final Session session;
    private final ITransaction transaction;
    private int entryCount;

    public SecuredTransaction(Session session, ITransaction transaction) {
        Assert.notNull(session);
        Assert.notNull(transaction);

        this.session = session;
        this.transaction = transaction;
    }

    public ITransaction getTransaction() {
        return transaction;
    }

    public boolean isPrivileged() {
        return entryCount > 0;
    }

    public boolean beginEntry() {
        boolean res = entryCount == 0;
        entryCount++;
        return res;
    }

    public void endEntry() {
        entryCount--;
    }

    @Override
    public boolean isReadOnly() {
        return transaction.isReadOnly();
    }

    @Override
    public int getOptions() {
        return transaction.getOptions();
    }

    @Override
    public <T> T getOperation() {
        return transaction.getOperation();
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public IPrincipal getPrincipal() {
        return session.getPrincipal();
    }

    @Override
    public <T> T findDomainService(String name) {
        IDomainService domainService = transaction.findDomainService(name);
        if (domainService != null && domainService.getSchema().getConfiguration().isSecured())
            return (T) domainService;
        else
            return null;
    }

    @Override
    public <T> T runPrivileged(Callable<T> operation) {
        beginEntry();

        try {
            return operation.call();
        } catch (Exception e) {
            return Exceptions.wrapAndThrow(e);
        } finally {
            endEntry();
        }
    }

    @Override
    public void runPrivileged(Runnable operation) {
        beginEntry();

        try {
            operation.run();
        } finally {
            endEntry();
        }
    }
}
