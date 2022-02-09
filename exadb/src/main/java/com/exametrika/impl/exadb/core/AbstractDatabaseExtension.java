/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDatabaseExtension;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.core.IPublicExtensionRegistrar;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;


/**
 * The {@link AbstractDatabaseExtension} represents an abstract database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractDatabaseExtension implements IDatabaseExtension {
    private final String name;

    public AbstractDatabaseExtension(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    @Override
    public Set<ModuleSchemaConfiguration> getRequiredModules() {
        return Collections.emptySet();
    }

    @Override
    public Set<ModuleSchemaConfiguration> getOptionalModules() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, IDataMigrator> getDataMigrators() {
        return null;
    }

    @Override
    public DatabaseExtensionSchemaConfiguration getSchema() {
        return null;
    }

    @Override
    public void setSchema(DatabaseExtensionSchemaConfiguration schema) {
    }

    @Override
    public DatabaseExtensionConfiguration getConfiguration() {
        return new DatabaseExtensionConfiguration(name);
    }

    @Override
    public void setConfiguration(DatabaseExtensionConfiguration configuration, boolean clearCache) {
    }

    @Override
    public void registerPublicExtensions(IPublicExtensionRegistrar registrar) {
    }

    @Override
    public <T> T getOperationManager() {
        return null;
    }

    @Override
    public ICacheControl getCacheControl() {
        return null;
    }

    @Override
    public IExtensionSpace getExtensionSpace() {
        return null;
    }

    @Override
    public void start(IDatabaseContext context) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void onTimer(long currentTime) {
    }

    @Override
    public String printStatistics() {
        return "";
    }
}
