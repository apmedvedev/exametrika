/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.config;

import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link PeriodConfigurationExtention} is a helper class that is used to load {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new PeriodConfigurationLoader();
        parameters.typeLoaders.put("PeriodDatabaseExtension", processor);
        parameters.schemaMappings.put(PeriodDatabaseExtensionConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(PeriodDatabaseExtensionConfiguration.class) + "/perfdb.schema", false));
        return parameters;
    }
}
