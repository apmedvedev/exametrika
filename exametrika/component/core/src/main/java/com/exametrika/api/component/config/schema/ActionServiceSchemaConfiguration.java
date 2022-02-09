/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.impl.component.services.ActionService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link ActionServiceSchemaConfiguration} represents a configuration of action execution service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "ActionService";

    public ActionServiceSchemaConfiguration() {
        super(NAME, NAME, "Action service.");
    }

    @Override
    public IDomainService createService() {
        return new ActionService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new ActionServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ActionServiceSchemaConfiguration))
            return false;

        ActionServiceSchemaConfiguration configuration = (ActionServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}