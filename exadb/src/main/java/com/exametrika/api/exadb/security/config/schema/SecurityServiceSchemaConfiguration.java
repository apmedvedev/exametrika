/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.security.SecurityService;
import com.exametrika.impl.exadb.security.schema.SecurityServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link SecurityServiceSchemaConfiguration} represents a configuration of security service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    private static final String NAME = "SecurityService";
    private final SecuritySchemaConfiguration securityModel;

    public SecurityServiceSchemaConfiguration(SecuritySchemaConfiguration securityModel) {
        super(NAME, NAME, "Security service.");

        Assert.notNull(securityModel);

        this.securityModel = securityModel;
    }

    public SecuritySchemaConfiguration getSecurityModel() {
        return securityModel;
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new SecurityServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new SecurityService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new SecurityServiceSchemaConfiguration(securityModel.combine(((SecurityServiceSchemaConfiguration) schema).securityModel));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecurityServiceSchemaConfiguration))
            return false;

        SecurityServiceSchemaConfiguration configuration = (SecurityServiceSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(securityModel, configuration.securityModel);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(securityModel);
    }
}