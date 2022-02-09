/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.config.schema.ComponentServiceSchemaConfiguration;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link ComponentServiceSchema} represents a schema of component service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentServiceSchema extends DomainServiceSchema {
    private IPermission createGroupsPermission;

    public ComponentServiceSchema(IDatabaseContext context, ComponentServiceSchemaConfiguration configuration) {
        super(context, configuration);
    }

    @Override
    public ComponentServiceSchemaConfiguration getConfiguration() {
        return (ComponentServiceSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        createGroupsPermission = Permissions.permission(this, "componentService:create:groups", true);
    }

    public IPermission getCreateGroupsPermission() {
        return createGroupsPermission;
    }
}
