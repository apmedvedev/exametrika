/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.tx;

import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DbBatchControl} is a batch control.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DbBatchControl implements IBatchControl {
    private final DbBatchOperation batchOperation;
    private final IRawBatchControl batchControl;

    public DbBatchControl(DbBatchOperation batchOperation, IRawBatchControl batchControl) {
        Assert.notNull(batchOperation);
        Assert.notNull(batchControl);

        this.batchOperation = batchOperation;
        this.batchControl = batchControl;
    }

    @Override
    public boolean isPageCachingEnabled() {
        return batchControl.isPageCachingEnabled();
    }

    @Override
    public void setPageCachingEnabled(boolean value) {
        batchControl.setPageCachingEnabled(value);
    }

    @Override
    public int getNonCachedPagesInvalidationQueueSize() {
        return batchControl.getNonCachedPagesInvalidationQueueSize();
    }

    @Override
    public void setNonCachedPagesInvalidationQueueSize(int value) {
        batchControl.setNonCachedPagesInvalidationQueueSize(value);
    }

    @Override
    public void setMaxPageCacheSize(int pageTypeIndex, String category, long value) {
        batchControl.setMaxPageCacheSize(pageTypeIndex, category, value);
    }

    @Override
    public boolean canContinue() {
        return batchControl.canContinue();
    }

    @Override
    public boolean isCachingEnabled() {
        return batchOperation.isCachingEnabled();
    }

    @Override
    public void setCachingEnabled(boolean value) {
        batchOperation.setCachingEnabled(value);
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
        batchOperation.setMaxCacheSize(category, value);
    }
}