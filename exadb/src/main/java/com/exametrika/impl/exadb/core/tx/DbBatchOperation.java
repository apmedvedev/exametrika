/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link DbBatchOperation} is a batch operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DbBatchOperation implements IRawBatchOperation {
    private final IBatchOperation operation;
    private boolean cachingEnabled = true;
    private Set<CacheConstraint> constraints;
    private SchemaSpace schemaSpace;

    public static final class CacheConstraint implements Serializable {
        public final String category;
        public long maxCacheSize;

        public CacheConstraint(String category, long maxCacheSize) {
            Assert.notNull(category);

            this.category = category;
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof CacheConstraint))
                return false;

            CacheConstraint constraint = (CacheConstraint) o;
            return category.equals(constraint.category);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(category);
        }

        @Override
        public String toString() {
            return category;
        }
    }

    public DbBatchOperation(IBatchOperation operation, boolean cachingEnabled, Set<CacheConstraint> constraints) {
        Assert.notNull(operation);

        this.operation = operation;
        this.cachingEnabled = cachingEnabled;
        this.constraints = constraints;
    }

    public IBatchOperation getOperation() {
        return operation;
    }

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public void setCachingEnabled(boolean value) {
        this.cachingEnabled = value;

        schemaSpace.getDatabase().getContext().getCacheControl().setCachingEnabled(value);
    }

    public Set<CacheConstraint> getConstraints() {
        return constraints;
    }

    public void setMaxCacheSize(String category, long value) {
        if (constraints == null)
            constraints = new HashSet<CacheConstraint>();

        constraints.add(new CacheConstraint(category, value));

        schemaSpace.getDatabase().getContext().getCacheControl().setMaxCacheSize(category, value);
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
    public void setContext(IRawBatchContext context) {
        schemaSpace = (SchemaSpace) ((IDatabaseContext) context.getContext()).getSchemaSpace();
    }

    @Override
    public void onBeforeStarted(IRawTransaction transaction) {
        Spaces.bindSystemFiles(schemaSpace.getDatabase().getContext(), transaction);

        Transaction exaTransaction = new Transaction(transaction, operation.getOptions(),
                schemaSpace.getDatabase(), schemaSpace.getExtensionManager(), schemaSpace.getDomainServiceManager(), operation);

        TransactionManager transactionManager = schemaSpace.getTransactionManager();
        transactionManager.setTransaction(exaTransaction);

        getCacheControl().onTransactionStarted();
    }

    @Override
    public void validate(IRawTransaction transaction) {
        TransactionManager transactionManager = schemaSpace.getTransactionManager();
        Transaction exaTransaction = transactionManager.getTransaction();
        operation.validate(exaTransaction);
    }

    @Override
    public boolean run(IRawTransaction transaction, IRawBatchControl batchControl) {
        TransactionManager transactionManager = schemaSpace.getTransactionManager();
        Transaction exaTransaction = transactionManager.getTransaction();

        schemaSpace.getDatabase().getContext().getCacheControl().setCachingEnabled(cachingEnabled);
        enableConstraints(true);

        return operation.run(exaTransaction, new DbBatchControl(this, batchControl));
    }

    @Override
    public void onBeforeCommitted(boolean completed) {
        getCacheControl().onTransactionCommitted();
        enableConstraints(false);
        schemaSpace.getDatabase().getContext().getCacheControl().setCachingEnabled(true);
    }

    @Override
    public void onCommitted(boolean completed) {
        if (completed)
            operation.onCommitted();

        schemaSpace.getTransactionManager().setTransaction(null);
    }

    @Override
    public boolean onBeforeRolledBack() {
        return getCacheControl().onBeforeTransactionRolledBack();
    }

    @Override
    public void onRolledBack(boolean clearCache) {
        enableConstraints(false);
        getCacheControl().onTransactionRolledBack();
        if (clearCache)
            getCacheControl().clear(true);

        operation.onRolledBack();

        schemaSpace.getTransactionManager().setTransaction(null);
    }

    private ICacheControl getCacheControl() {
        return schemaSpace.getDatabase().getContext().getCacheControl();
    }

    private void enableConstraints(boolean enable) {
        if (constraints == null)
            return;

        for (CacheConstraint constraint : constraints) {
            schemaSpace.getDatabase().getContext().getCacheControl().setMaxCacheSize(constraint.category,
                    enable ? constraint.maxCacheSize : Long.MAX_VALUE);
        }
    }
}