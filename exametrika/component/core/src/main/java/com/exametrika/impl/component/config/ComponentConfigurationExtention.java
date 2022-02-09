/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.config;

import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ComponentConfigurationExtention} is a helper class that is used to load {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new ComponentConfigurationLoader();
        parameters.typeLoaders.put("AlertService", processor);
        parameters.schemaMappings.put(AlertServiceConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(AlertServiceConfiguration.class) + "/component.schema", false));
        return parameters;
    }
}
