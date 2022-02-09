/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.config;

import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;


/**
 * The {@link PeriodConfigurationLoader} is a loader of {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        if (type.equals("PeriodDatabaseExtension"))
            return new PeriodDatabaseExtensionConfiguration();
        else
            throw new InvalidConfigurationException();
    }
}