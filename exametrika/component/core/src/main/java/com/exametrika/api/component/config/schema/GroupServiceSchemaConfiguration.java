/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.impl.component.schema.GroupServiceSchema;
import com.exametrika.impl.component.services.GroupService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link GroupServiceSchemaConfiguration} represents a configuration of group service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GroupServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "GroupService";

    public GroupServiceSchemaConfiguration() {
        super(NAME, NAME, "Group service.");
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new GroupServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new GroupService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new GroupServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupServiceSchemaConfiguration))
            return false;

        GroupServiceSchemaConfiguration configuration = (GroupServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}