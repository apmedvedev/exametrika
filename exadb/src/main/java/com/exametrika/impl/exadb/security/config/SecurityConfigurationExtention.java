/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.config;

import com.exametrika.api.exadb.security.config.SecurityServiceConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link SecurityConfigurationExtention} is a helper class that is used to load {@link SecurityServiceConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new SecurityConfigurationLoader();
        parameters.typeLoaders.put("SecurityService", processor);
        parameters.schemaMappings.put(SecurityServiceConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(SecurityServiceConfiguration.class) + "/security.schema", false));
        return parameters;
    }
}
