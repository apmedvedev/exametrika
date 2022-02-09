/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.schema;

import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;


/**
 * The {@link IDomainServiceSchema} represents a domain service schema.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDomainServiceSchema extends ISchemaObject {
    String TYPE = "domainService";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    DomainServiceSchemaConfiguration getConfiguration();

    /**
     * Returns domain.
     *
     * @return domain
     */
    @Override
    IDomainSchema getParent();

    /**
     * Returns domain service.
     *
     * @param <T> domain service type
     * @return domain service
     */
    <T> T getService();
}
