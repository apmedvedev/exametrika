/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.config;

import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link IndexConfigurationExtention} is a helper class that is used to load {@link IndexDatabaseExtensionConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new IndexConfigurationLoader();
        parameters.typeLoaders.put("IndexDatabaseExtension", processor);
        parameters.schemaMappings.put(IndexDatabaseExtensionConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(IndexDatabaseExtensionConfiguration.class) + "/index.schema", false));
        return parameters;
    }
}
