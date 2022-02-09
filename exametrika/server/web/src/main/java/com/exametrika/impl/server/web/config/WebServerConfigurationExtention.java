/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web.config;

import com.exametrika.api.server.web.config.WebServerConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link WebServerConfigurationExtention} is a helper class that is used to load {@link WebServerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class WebServerConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("webServer", new WebServerConfigurationLoader());
        parameters.contextFactories.put(WebServerConfiguration.SCHEMA, new WebServerLoadContext());
        parameters.schemaMappings.put(WebServerConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(WebServerConfiguration.class) + "/webServer.schema", false));
        parameters.validators.put("webServer", new WebServerValidator());
        parameters.topLevelElements.put("webServer", new Pair("WebServer", false));
        return parameters;
    }
}
