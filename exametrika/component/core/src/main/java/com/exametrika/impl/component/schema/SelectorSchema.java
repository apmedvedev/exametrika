/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link SelectorSchema} represents a selector schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SelectorSchema implements ISelectorSchema {
    private final String name;
    private final SelectorSchemaConfiguration configuration;
    private final IDatabaseContext context;
    private final IPermission executePermission;

    public SelectorSchema(String name, SelectorSchemaConfiguration configuration, IDatabaseContext context, ISchemaObject component) {
        Assert.notNull(name);
        Assert.notNull(configuration);

        this.name = name;
        this.configuration = configuration;
        this.context = context;

        executePermission = Permissions.permission(component, "component:" + component.getConfiguration().getName() +
                ":execute:selector:" + name, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SelectorSchemaConfiguration getConfiguration() {
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
