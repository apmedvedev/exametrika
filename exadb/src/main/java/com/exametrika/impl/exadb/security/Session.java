/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.security.ISecuredOperation;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.core.Database;


/**
 * The {@link Session} is a database security session.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Session implements ISession {
    private final Element<Session> element = new Element<Session>(this);
    private final Database database;
    private final SecurityService securityService;
    private final Principal principal;
    private long lastAccessTime;
    private SecuredTransaction transaction;
    private volatile boolean opened = true;

    public Session(Database database, SecurityService securityService, Principal principal) {
        Assert.notNull(database);
        Assert.notNull(securityService);
        Assert.notNull(principal);

        this.database = database;
        this.securityService = securityService;
        this.principal = principal;
        lastAccessTime = Times.getCurrentTime();
    }

    public Database getDatabase() {
        return database;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public SecuredTransaction getTransaction() {
        return transaction;
    }

    public void activate(SecuredTransaction transaction) {
        lastAccessTime = Times.getCurrentTime();
        this.transaction = transaction;
        securityService.getSessionManager().setCurrentSession(this);
    }

    public void deactivate() {
        this.transaction = null;
        securityService.getSessionManager().setCurrentSession(null);
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public Element<Session> getElement() {
        return element;
    }

    @Override
    public boolean isOpened() {
        return opened;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!opened)
                return;

            opened = false;
        }

        if (!((Compartment) database.getContext().getCompartment()).isMainThread()) {
            database.transaction(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    closeSession();
                }
            });
        } else
            closeSession();
    }

    @Override
    public void transaction(IOperation operation) {
        Assert.checkState(opened);

        database.transaction(new DbOperation(this, operation));
    }

    @Override
    public void transactionSync(IOperation operation) {
        Assert.checkState(opened);

        database.transactionSync(new DbOperation(this, operation));
    }

    @Override
    public void transaction(IBatchOperation operation) {
        Assert.checkState(opened);

        database.transaction(new DbBatchOperation(this, operation));
    }

    @Override
    public void transactionSync(IBatchOperation operation) {
        Assert.checkState(opened);

        database.transactionSync(new DbBatchOperation(this, operation));
    }

    @Override
    public void transaction(ISchemaOperation operation) {
        Assert.checkState(opened);

        database.transaction(new DbSchemaOperation(this, operation));
    }

    @Override
    public void transactionSync(ISchemaOperation operation) {
        Assert.checkState(opened);

        database.transactionSync(new DbSchemaOperation(this, operation));
    }

    @Override
    public void transaction(ISecuredOperation operation) {
        Assert.checkState(opened);

        database.transaction(new DbSecuredOperation(this, operation));
    }

    @Override
    public void transactionSync(ISecuredOperation operation) {
        Assert.checkState(opened);

        database.transactionSync(new DbSecuredOperation(this, operation));
    }

    private void closeSession() {
        element.remove();

        if (securityService.isAuditEnabled())
            securityService.addAuditRecord(new AuditRecord(principal.getUser().getName(), "session:close", null, Times.getCurrentTime(), true));
    }
}
