/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.impl.component.services.SelectionService;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link SelectionServiceSchemaConfiguration} represents a configuration of selection service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SelectionServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "SelectionService";

    public SelectionServiceSchemaConfiguration() {
        super(NAME, NAME, "Selection service.");
    }

    @Override
    public boolean isSecured() {
        return false;
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new DomainServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new SelectionService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new SelectionServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SelectionServiceSchemaConfiguration))
            return false;

        SelectionServiceSchemaConfiguration configuration = (SelectionServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}