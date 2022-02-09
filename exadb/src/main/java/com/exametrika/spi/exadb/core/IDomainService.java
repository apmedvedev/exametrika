/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link IDomainService} represents a control interface of domain service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IDomainService {
    /**
     * Returns domain service schema.
     *
     * @return domain service schema
     */
    IDomainServiceSchema getSchema();

    /**
     * Sets schema of domain service.
     *
     * @param schema schema of domain service
     */
    void setSchema(IDomainServiceSchema schema);

    /**
     * Returns domain service configuration.
     *
     * @return domain service configuration
     */
    DomainServiceConfiguration getConfiguration();

    /**
     * Sets configuration of domain service.
     *
     * @param configuration domain service configuration or null if default configuration is set
     * @param clearCache    if true internal caches must be cleared
     */
    void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache);

    /**
     * Starts domain service.
     *
     * @param context database context
     */
    void start(IDatabaseContext context);

    /**
     * Stops domain service.
     */
    void stop();

    /**
     * Called after start, when new domain service has been created.
     */
    void onCreated();

    /**
     * Called after start, when existing domain service has been opened.
     */
    void onOpened();

    /**
     * Called before stop, when domain service is about to be deleted.
     */
    void onDeleted();

    /**
     * Called when domain service timer is elapsed.
     *
     * @param currentTime currentTime
     */
    void onTimer(long currentTime);

    /**
     * Clears all internal caches of domain service.
     */
    void clearCaches();
}
