/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.impl.component.services.BehaviorTypeService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link BehaviorTypeServiceSchemaConfiguration} represents a configuration of behavior type service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BehaviorTypeServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "BehaviorTypeProvider";

    public BehaviorTypeServiceSchemaConfiguration() {
        super(NAME, NAME, "Behavior type service.");
    }

    @Override
    public IDomainService createService() {
        return new BehaviorTypeService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new BehaviorTypeServiceSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BehaviorTypeServiceSchemaConfiguration))
            return false;

        BehaviorTypeServiceSchemaConfiguration configuration = (BehaviorTypeServiceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}