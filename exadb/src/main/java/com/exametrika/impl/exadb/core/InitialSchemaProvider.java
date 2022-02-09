/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;


/**
 * The {@link InitialSchemaProvider} represents an default implementation of initial schema provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InitialSchemaProvider implements IInitialSchemaProvider {
    private final ModularDatabaseSchemaConfiguration configuration;

    public InitialSchemaProvider(ModularDatabaseSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public ModularDatabaseSchemaConfiguration getInitialSchema() {
        return configuration;
    }
}
