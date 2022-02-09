/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.GroupServiceSchemaConfiguration;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupServiceSchema} represents a schema of group service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupServiceSchema extends DomainServiceSchema {
    private ComponentModelSchemaConfiguration componentModel;

    public GroupServiceSchema(IDatabaseContext context, GroupServiceSchemaConfiguration configuration) {
        super(context, configuration);
    }

    public ComponentModelSchemaConfiguration getComponentModel() {
        return componentModel;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        ComponentServiceSchemaConfiguration configuration = (ComponentServiceSchemaConfiguration) getParent().findDomainService("ComponentService").getConfiguration();
        componentModel = configuration.getComponentModel();
    }
}
