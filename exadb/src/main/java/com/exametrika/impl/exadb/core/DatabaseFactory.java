/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DatabaseFactory} is a factory of exa database.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseFactory implements IDatabaseFactory {
    @Override
    public Database createDatabase(Parameters parameters, DatabaseConfiguration configuration) {
        Assert.notNull(configuration);

        if (parameters == null)
            parameters = new Parameters();

        return new Database(parameters, configuration);
    }
}
