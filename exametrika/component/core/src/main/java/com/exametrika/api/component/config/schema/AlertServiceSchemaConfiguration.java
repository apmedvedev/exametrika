/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.impl.component.services.AlertService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link AlertServiceSchemaConfiguration} represents a configuration of alert service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "AlertService";

    public AlertServiceSchemaConfiguration() {
        super(NAME, NAME, "Alert service.");
    }

    @Override
    public IDomainService createService() {
        return new AlertService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new AlertServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertServiceSchemaConfiguration))
            return false;

        AlertServiceSchemaConfiguration configuration = (AlertServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}