/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;

/**
 * The {@link AbstractIndexSpace} is an abstract index implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractIndexSpace implements IIndex {
    private final int id;
    protected final IndexManager indexManager;
    protected final IndexSchemaConfiguration schema;
    protected long lastAccessTime;
    protected boolean stale;

    protected AbstractIndexSpace(IndexManager indexManager, IndexSchemaConfiguration schema, int id) {
        Assert.notNull(schema);

        this.indexManager = indexManager;
        this.schema = schema;
        this.id = id;

        if (indexManager != null)
            lastAccessTime = indexManager.getTimeService().getCurrentTime();
    }

    @Override
    public final IndexSchemaConfiguration getSchema() {
        return schema;
    }

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final boolean isStale() {
        if (stale)
            return true;
        else if (indexManager != null) {
            lastAccessTime = indexManager.getTimeService().getCurrentTime();
            return false;
        } else
            return false;
    }

    @Override
    public final void refresh() {
        Assert.checkState(!isStale());
    }

    @Override
    public void unload() {
        if (isStale())
            return;

        if (indexManager != null)
            indexManager.unloadIndex(id);
    }

    @Override
    public void delete() {
        if (indexManager != null)
            indexManager.deleteIndex(id);
    }

    public final void setStale() {
        this.stale = true;
    }

    public final long getLastAccessTime() {
        return lastAccessTime;
    }

    public void onTransactionStarted() {
    }

    public void onTransactionCommitted() {
    }

    public boolean onBeforeTransactionRolledBack() {
        return false;
    }

    public void onTransactionRolledBack() {
    }

    public void onTimer(long currentTime) {
    }

    public void flush(boolean full) {
    }

    public void unload(boolean full) {
    }

    public void assertValid() {
    }

    public String printStatistics() {
        return "";
    }

    public CacheCategoryTypeConfiguration getCategoryTypeConfiguration() {
        return null;
    }

    public void setCategoryTypeConfiguration(CacheCategoryTypeConfiguration defaultCacheCategoryType) {
    }

    public void setConfiguration(IndexDatabaseExtensionConfiguration configuration) {
    }

    @Override
    public String toString() {
        return schema.getAlias();
    }
}
