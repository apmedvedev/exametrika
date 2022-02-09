/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.impl.component.schema.UserInterfaceServiceSchema;
import com.exametrika.impl.component.services.UserInterfaceService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link UserInterfaceServiceSchemaConfiguration} represents a configuration of user interface service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserInterfaceServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "UserInterfaceService";

    public UserInterfaceServiceSchemaConfiguration() {
        super(NAME, NAME, "User interface service.");
    }

    @Override
    public boolean isSecured() {
        return true;
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new UserInterfaceServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new UserInterfaceService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new UserInterfaceServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UserInterfaceServiceSchemaConfiguration))
            return false;

        UserInterfaceServiceSchemaConfiguration configuration = (UserInterfaceServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}