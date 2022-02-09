/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;

/**
 * The {@link DomainServiceSchemaConfiguration} represents a configuration of domain service schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DomainServiceSchemaConfiguration extends SchemaConfiguration {
    public DomainServiceSchemaConfiguration(String name) {
        this(name, name, null);
    }

    public DomainServiceSchemaConfiguration(String name, String alias, String description) {
        super(name, alias, description);
    }

    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new DomainServiceSchema(context, this);
    }

    public boolean isSecured() {
        return false;
    }

    public abstract IDomainService createService();

    public boolean isCompatible(DomainServiceSchemaConfiguration newSchema) {
        return true;
    }

    public void freeze() {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DomainServiceSchemaConfiguration))
            return false;

        DomainServiceSchemaConfiguration configuration = (DomainServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
