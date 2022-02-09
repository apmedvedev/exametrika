/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.impl.component.schema.HealthServiceSchema;
import com.exametrika.impl.component.services.HealthService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link HealthServiceSchemaConfiguration} represents a configuration of health service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "HealthService";

    public HealthServiceSchemaConfiguration() {
        super(NAME, NAME, "Health service.");
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new HealthServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new HealthService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new HealthServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HealthServiceSchemaConfiguration))
            return false;

        HealthServiceSchemaConfiguration configuration = (HealthServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}