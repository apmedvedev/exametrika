/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.schema;

import java.util.List;

import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;


/**
 * The {@link IDomainSchema} represents a domain schema.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDomainSchema extends ISchemaObject {
    String TYPE = "domain";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    DomainSchemaConfiguration getConfiguration();

    /**
     * Returns database.
     *
     * @return database
     */
    @Override
    IDatabaseSchema getParent();

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
     * Returns list of schemas of database spaces.
     *
     * @return database spaces schemas
     */
    List<ISpaceSchema> getSpaces();

    /**
     * Finds space schema by name.
     *
     * @param <T>  space schema type
     * @param name name of space
     * @return space schema or null if space is not found
     */
    <T extends ISpaceSchema> T findSpace(String name);

    /**
     * Finds space schema by alias.
     *
     * @param <T>   space schema type
     * @param alias alias of space
     * @return space schema or null if space is not found
     */
    <T extends ISpaceSchema> T findSpaceByAlias(String alias);

    /**
     * Returns list of schemas of domain services.
     *
     * @return domain services schemas
     */
    List<IDomainServiceSchema> getDomainServices();

    /**
     * Finds domain service schema by name.
     *
     * @param <T>  domain service schema type
     * @param name name of service
     * @return domain service schema or null if service is not found
     */
    <T extends IDomainServiceSchema> T findDomainService(String name);

    /**
     * Finds domain service schema by alias.
     *
     * @param <T>   domain service schema type
     * @param alias alias of domain service
     * @return domain service schema or null if domain service is not found
     */
    <T extends IDomainServiceSchema> T findDomainServiceByAlias(String alias);
}
