/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import com.exametrika.api.exadb.objectdb.IObjectOperationManager;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.impl.exadb.core.AbstractDatabaseExtension;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeManager;
import com.exametrika.impl.exadb.objectdb.ops.ObjectOperationManager;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IPublicExtensionRegistrar;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;


/**
 * The {@link ObjectDatabaseExtension} represents an object database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectDatabaseExtension extends AbstractDatabaseExtension {
    private ObjectOperationManager operationManager;
    private ObjectDatabaseExtensionConfiguration configuration = new ObjectDatabaseExtensionConfiguration(10000, 600000);
    private ObjectNodeManager nodeManager;
    private ObjectNodeCacheManager nodeCacheManager;
    private IDatabaseContext context;

    public ObjectDatabaseExtension() {
        super(ObjectDatabaseExtensionConfiguration.NAME);
    }

    public ObjectNodeManager getNodeManager() {
        return nodeManager;
    }

    public ObjectNodeCacheManager getNodeCacheManager() {
        return nodeCacheManager;
    }

    @Override
    public ObjectDatabaseExtensionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DatabaseExtensionConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new ObjectDatabaseExtensionConfiguration(10000, 600000);

        this.configuration = (ObjectDatabaseExtensionConfiguration) configuration;

        if (nodeManager != null) {
            nodeManager.setConfiguration(this.configuration);
            nodeCacheManager.setConfiguration(context.getConfiguration(), clearCache);
        }
    }

    @Override
    public IObjectOperationManager getOperationManager() {
        return operationManager;
    }

    @Override
    public ICacheControl getCacheControl() {
        return nodeManager;
    }

    @Override
    public void registerPublicExtensions(IPublicExtensionRegistrar registrar) {
        registrar.register(IObjectOperationManager.NAME, operationManager, false);
    }

    @Override
    public synchronized void start(IDatabaseContext context) {
        this.context = context;
        nodeManager = new ObjectNodeManager(context, context.getTimeService(), configuration);
        nodeCacheManager = new ObjectNodeCacheManager(context, nodeManager);
        nodeManager.setNodeCacheManager(nodeCacheManager);
        operationManager = new ObjectOperationManager(nodeCacheManager, context);
        operationManager.start();
    }

    @Override
    public synchronized void stop() {
        if (operationManager == null)
            return;

        operationManager.stop();
        operationManager = null;
        nodeManager = null;
        nodeCacheManager.close();
        nodeCacheManager = null;
    }

    @Override
    public void onTimer(long currentTime) {
        nodeCacheManager.onTimer(currentTime);
    }

    @Override
    public String printStatistics() {
        return nodeCacheManager.printStatistics();
    }
}
