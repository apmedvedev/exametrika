/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.schema.ComponentServiceSchema;
import com.exametrika.impl.component.services.ComponentService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link ComponentServiceSchemaConfiguration} represents a configuration of component service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "ComponentService";
    private final ComponentModelSchemaConfiguration componentModel;

    public ComponentServiceSchemaConfiguration(ComponentModelSchemaConfiguration componentModel) {
        super(NAME, NAME, "Component service.");

        Assert.notNull(componentModel);

        this.componentModel = componentModel;
    }

    public ComponentModelSchemaConfiguration getComponentModel() {
        return componentModel;
    }

    @Override
    public boolean isSecured() {
        return true;
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new ComponentServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new ComponentService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new ComponentServiceSchemaConfiguration(componentModel.combine(((ComponentServiceSchemaConfiguration) schema).componentModel));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentServiceSchemaConfiguration))
            return false;

        ComponentServiceSchemaConfiguration configuration = (ComponentServiceSchemaConfiguration) o;
        return super.equals(configuration) && componentModel.equals(configuration.componentModel);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentModel);
    }
}