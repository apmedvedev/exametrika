/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;


/**
 * The {@link ISchemaTransaction} represents a transaction for schema modification.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchemaTransaction {
    /**
     * Returns current combined database schema configuration.
     *
     * @return current combined database schema configuration or null if schema configuration is not set
     */
    ModularDatabaseSchemaConfiguration getConfiguration();

    /**
     * Sets new database alias.
     *
     * @param alias new database alias
     */
    void setDatabaseAlias(String alias);

    /**
     * Sets new database description.
     *
     * @param description new database description
     */
    void setDatabaseDescription(String description);

    /**
     * Adds new or updates existing module schema.
     *
     * @param schema        new module schema
     * @param dataMigrators map of custom data migrators by fully qualified space name or null if data migrators are not used.
     *                      If data migrator is not set for some particular space or null is set for all data migrators default
     *                      data migration rules are used
     */
    void addModule(ModuleSchemaConfiguration schema, Map<String, ? extends IDataMigrator> dataMigrators);

    /**
     * Adds new or updates existing schemas of modules loaded from specified path.
     *
     * @param schemaPath    modules schemas
     * @param dataMigrators map of custom data migrators by fully qualified space name or null if data migrators are not used.
     *                      If data migrator is not set for some particular space or null is set for all data migrators default
     *                      data migration rules are used
     */
    void addModules(String schemaPath, Map<String, ? extends IDataMigrator> dataMigrators);

    /**
     * Adds optional extension modules.
     *
     * @param names         module names
     * @param dataMigrators map of custom data migrators by fully qualified space name or null if data migrators are not used.
     *                      If data migrator is not set for some particular space or null is set for all data migrators default
     *                      data migration rules are used
     */
    void addExtensionModules(Set<String> names, Map<String, ? extends IDataMigrator> dataMigrators);

    /**
     * Removes module schema.
     *
     * @param name module name
     */
    void removeModule(String name);

    /**
     * Removes all modules schemas.
     */
    void removeAllModules();

    /**
     * Finds public transaction extension by name.
     *
     * @param name public transaction extension name
     * @return extension or null if extension is not found
     */
    <T> T findExtension(String name);
}
