/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link DatabaseConfigurationExtention} is a helper class that is used to load {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new DatabaseConfigurationLoader();
        parameters.typeLoaders.put("Database", processor);
        parameters.schemaMappings.put(DatabaseConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(DatabaseConfiguration.class) + "/exadb.schema", false));
        return parameters;
    }
}
