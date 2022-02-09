/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;


/**
 * The {@link IDatabaseExtension} represents a database extension.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDatabaseExtension {
    /**
     * Returns modules of database required by extension.
     *
     * @return modules of database required by extension
     */
    Set<ModuleSchemaConfiguration> getRequiredModules();

    /**
     * Returns optional modules of database supported but not required by extension.
     *
     * @return optional modules of database supported but not required by extension
     */
    Set<ModuleSchemaConfiguration> getOptionalModules();

    /**
     * Returns map of custom extensions's data migrators by fully qualified space name or null if data migrators are not used.
     * If data migrator is not set for some particular space or null is set for all data migrators default
     * data migration rules are used
     *
     * @return map custom data migrators defined by extension for data migration of required and optional modules of extension or null
     * if data migrators are not used
     */
    Map<String, IDataMigrator> getDataMigrators();

    /**
     * Returns extension schema configuration.
     *
     * @return extension schema configuration or null if extension does not have schema
     */
    DatabaseExtensionSchemaConfiguration getSchema();

    /**
     * Sets configuration of extension schema.
     *
     * @param schema extension schema configuration or null if default configuration is set
     */
    void setSchema(DatabaseExtensionSchemaConfiguration schema);

    /**
     * Returns extension configuration.
     *
     * @return extension configuration
     */
    DatabaseExtensionConfiguration getConfiguration();

    /**
     * Sets configuration of extension.
     *
     * @param configuration extension configuration or null if default configuration is set
     * @param clearCache    if true internal caches must be cleared
     */
    void setConfiguration(DatabaseExtensionConfiguration configuration, boolean clearCache);

    /**
     * Registers database public extensions implemented by this database extension.
     *
     * @param registrar registrar
     */
    void registerPublicExtensions(IPublicExtensionRegistrar registrar);

    /**
     * Returns operation manager.
     *
     * @param <T> operation manager type
     * @return operation manager or null if extension does not have operation manager
     */
    <T> T getOperationManager();

    /**
     * Returns cache control of extension.
     *
     * @return cache control or null if extension does not have cache control
     */
    ICacheControl getCacheControl();

    /**
     * Returns extension space.
     *
     * @return extension space or null if extension does not have extension space
     */
    IExtensionSpace getExtensionSpace();

    /**
     * Starts extension.
     *
     * @param context database context
     */
    void start(IDatabaseContext context);

    /**
     * Stops extension.
     */
    void stop();

    /**
     * Called when extension timer is elapsed.
     *
     * @param currentTime currentTime
     */
    void onTimer(long currentTime);

    /**
     * Prints internal debug statistics.
     *
     * @return internal debug statistics
     */
    String printStatistics();
}
