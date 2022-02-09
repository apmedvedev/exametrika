/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IIndexOperationManager;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.utils.HolderManager;
import com.exametrika.impl.exadb.core.AbstractDatabaseExtension;
import com.exametrika.impl.exadb.index.ops.IndexOperationManager;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.core.IPublicExtensionRegistrar;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.index.IIndexDatabaseExtension;


/**
 * The {@link IndexDatabaseExtension} represents an index database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexDatabaseExtension extends AbstractDatabaseExtension implements IIndexDatabaseExtension {
    private IndexManager indexManager;
    private IndexOperationManager operationManager;
    private IndexDatabaseExtensionConfiguration configuration = new IndexDatabaseExtensionConfiguration(60000, new FullTextIndexConfiguration());
    private final HolderManager holderManager = new HolderManager();

    public IndexDatabaseExtension() {
        super(IndexDatabaseExtensionConfiguration.NAME);
    }

    public HolderManager getHolderManager() {
        return holderManager;
    }

    @Override
    public IIndexManager getIndexManager() {
        return indexManager;
    }

    @Override
    public IndexDatabaseExtensionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized void setConfiguration(DatabaseExtensionConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new IndexDatabaseExtensionConfiguration(60000, new FullTextIndexConfiguration());

        this.configuration = (IndexDatabaseExtensionConfiguration) configuration;

        if (indexManager != null)
            indexManager.setConfiguration(this.configuration);
    }

    @Override
    public IIndexOperationManager getOperationManager() {
        return operationManager;
    }

    @Override
    public ICacheControl getCacheControl() {
        return indexManager;
    }

    @Override
    public IExtensionSpace getExtensionSpace() {
        return indexManager;
    }

    @Override
    public void registerPublicExtensions(IPublicExtensionRegistrar registrar) {
        registrar.register(IIndexManager.NAME, indexManager, true);
        registrar.register(IIndexOperationManager.NAME, operationManager, false);
    }

    @Override
    public synchronized void start(IDatabaseContext context) {
        indexManager = new IndexManager(context, configuration);
        operationManager = new IndexOperationManager(context);
    }

    @Override
    public synchronized void stop() {
        if (operationManager == null)
            return;

        indexManager = null;
        operationManager = null;
        holderManager.close();
    }

    @Override
    public void onTimer(long currentTime) {
        indexManager.onTimer(currentTime);
    }

    @Override
    public String printStatistics() {
        return indexManager.printStatistics();
    }
}
