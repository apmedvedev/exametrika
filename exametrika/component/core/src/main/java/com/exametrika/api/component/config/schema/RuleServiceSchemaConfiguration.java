/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.impl.component.services.RuleService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link RuleServiceSchemaConfiguration} represents a configuration of rule service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RuleServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "RuleService";

    public RuleServiceSchemaConfiguration() {
        super(NAME, NAME, "Rule service.");
    }

    @Override
    public IDomainService createService() {
        return new RuleService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new RuleServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RuleServiceSchemaConfiguration))
            return false;

        RuleServiceSchemaConfiguration configuration = (RuleServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}