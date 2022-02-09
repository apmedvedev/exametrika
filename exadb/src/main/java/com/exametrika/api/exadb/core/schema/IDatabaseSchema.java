/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.schema;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;


/**
 * The {@link IDatabaseSchema} represents a database schema.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDatabaseSchema extends ISchemaObject {
    String TYPE = "database";

    /**
     * Returns modular configuration.
     *
     * @return modular configuration
     */
    ModularDatabaseSchemaConfiguration getModularConfiguration();

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    DatabaseSchemaConfiguration getConfiguration();

    /**
     * Returns database default time zone.
     *
     * @return database default time zone
     */
    TimeZone getTimeZone();

    /**
     * Returns database default locale.
     *
     * @return database default locale
     */
    Locale getLocale();

    /**
     * Returns creation time.
     *
     * @return creation time
     */
    long getCreationTime();

    /**
     * Returns version.
     *
     * @return version
     */
    int getVersion();

    /**
     * Returns list of schemas of database domains.
     *
     * @return database domains schemas
     */
    List<IDomainSchema> getDomains();

    /**
     * Finds domain schema by name.
     *
     * @param name name of domain
     * @return domain schema or null if domain is not found
     */
    IDomainSchema findDomain(String name);

    /**
     * Finds domain schema by alias.
     *
     * @param alias alias of domain
     * @return domain schema or null if space is not found
     */
    IDomainSchema findDomainByAlias(String alias);

    /**
     * Finds schema object by id.
     *
     * @param id identifier of schema object
     * @return child or null if child is not found
     */
    <T extends ISchemaObject> T findSchemaById(String id);
}
