/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.Arrays;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodDatabaseExtensionSchemaConfiguration;
import com.exametrika.impl.aggregator.cache.PeriodNodeCacheManager;
import com.exametrika.impl.aggregator.cache.PeriodNodeManager;
import com.exametrika.impl.aggregator.name.PeriodNameManager;
import com.exametrika.impl.aggregator.ops.PeriodOperationManager;
import com.exametrika.impl.exadb.core.AbstractDatabaseExtension;
import com.exametrika.impl.exadb.core.CompositeCacheControl;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.core.IPublicExtensionRegistrar;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;


/**
 * The {@link PeriodDatabaseExtension} represents an period database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodDatabaseExtension extends AbstractDatabaseExtension {
    private PeriodOperationManager operationManager;
    private PeriodDatabaseExtensionConfiguration configuration = new PeriodDatabaseExtensionConfiguration();
    private PeriodNodeManager nodeManager;
    private PeriodNodeCacheManager nodeCacheManager;
    private PeriodNameManager nameManager;
    private IDatabaseContext context;
    private ICacheControl cacheControl;
    private PeriodDatabaseExtensionSchemaConfiguration schema = new PeriodDatabaseExtensionSchemaConfiguration();

    public PeriodDatabaseExtension() {
        super(PeriodDatabaseExtensionConfiguration.NAME);
    }

    public PeriodNameManager getNameManager() {
        return nameManager;
    }

    public PeriodNodeManager getNodeManager() {
        return nodeManager;
    }

    public PeriodNodeCacheManager getNodeCacheManager() {
        return nodeCacheManager;
    }

    @Override
    public PeriodDatabaseExtensionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DatabaseExtensionConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new PeriodDatabaseExtensionConfiguration();

        this.configuration = (PeriodDatabaseExtensionConfiguration) configuration;

        if (nodeCacheManager != null)
            nodeCacheManager.setConfiguration(context.getConfiguration(), clearCache);

        if (nameManager != null)
            nameManager.setConfiguration(context.getConfiguration(), clearCache);
    }

    @Override
    public DatabaseExtensionSchemaConfiguration getSchema() {
        return schema;
    }

    @Override
    public void setSchema(DatabaseExtensionSchemaConfiguration schema) {
        if (schema == null)
            schema = new PeriodDatabaseExtensionSchemaConfiguration();

        this.schema = (PeriodDatabaseExtensionSchemaConfiguration) schema;
        nameManager.setSchema(this.schema.getNameSpace());
    }

    @Override
    public IPeriodOperationManager getOperationManager() {
        return operationManager;
    }

    @Override
    public ICacheControl getCacheControl() {
        return cacheControl;
    }

    @Override
    public IExtensionSpace getExtensionSpace() {
        return nameManager;
    }

    @Override
    public void registerPublicExtensions(IPublicExtensionRegistrar registrar) {
        registrar.register(IPeriodOperationManager.NAME, operationManager, false);
        registrar.register(IPeriodNameManager.NAME, nameManager, true);
    }

    @Override
    public synchronized void start(IDatabaseContext context) {
        this.context = context;
        nodeManager = new PeriodNodeManager(context, context.getTimeService());
        nodeCacheManager = new PeriodNodeCacheManager(context, nodeManager);
        nodeManager.setNodeCacheManager(nodeCacheManager);

        nameManager = new PeriodNameManager(context);

        cacheControl = new CompositeCacheControl(Arrays.asList(nodeManager, nameManager));

        operationManager = new PeriodOperationManager(nodeCacheManager, context);
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
        nameManager.close();
        nameManager = null;
        cacheControl = null;
    }

    @Override
    public void onTimer(long currentTime) {
        nodeCacheManager.onTimer(currentTime);
        nameManager.onTimer(currentTime);
    }

    @Override
    public String printStatistics() {
        return nodeCacheManager.printStatistics() + "\n" + nameManager.printStatistics();
    }
}
