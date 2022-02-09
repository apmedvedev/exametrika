/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DbSchemaOperation} is an implementation of database operation supporting schema operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DbSchemaOperation implements ISchemaOperation {
    private final Session session;
    private final ISchemaOperation operation;

    public DbSchemaOperation(Session session, ISchemaOperation operation) {
        Assert.notNull(session);
        Assert.notNull(operation);

        this.session = session;
        this.operation = operation;
    }

    @Override
    public int getSize() {
        return operation.getSize();
    }

    @Override
    public void run(ISchemaTransaction transaction) {
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
