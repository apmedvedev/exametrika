/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link ActionSchema} represents a action schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ActionSchema implements IActionSchema {
    private final String name;
    private final ActionSchemaConfiguration configuration;
    private final IDatabaseContext context;
    private final IPermission executePermission;

    public ActionSchema(String name, ActionSchemaConfiguration configuration, IDatabaseContext context, ISchemaObject component) {
        Assert.notNull(name);
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.name = name;
        this.configuration = configuration;
        this.context = context;

        executePermission = Permissions.permission(component, "component:" + component.getConfiguration().getName() + ":execute:action:" + name, true);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ActionSchemaConfiguration getConfiguration() {
        return configuration;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    @Override
    public IPermission getExecutePermission() {
        return executePermission;
    }
}
