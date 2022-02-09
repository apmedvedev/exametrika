/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ObjectConfigurationExtention} is a helper class that is used to load {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new ObjectConfigurationLoader();
        parameters.typeLoaders.put("ObjectDatabaseExtension", processor);
        parameters.schemaMappings.put(ObjectDatabaseExtensionConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ObjectDatabaseExtensionConfiguration.class) + "/objectdb.schema", false));
        return parameters;
    }
}
