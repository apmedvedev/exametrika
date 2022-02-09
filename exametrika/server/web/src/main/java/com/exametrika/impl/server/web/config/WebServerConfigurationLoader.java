/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web.config;

import com.exametrika.api.server.web.config.WebServerConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link WebServerConfigurationLoader} is a configuration loader for web server configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class WebServerConfigurationLoader extends AbstractElementLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        WebServerLoadContext loadContext = context.get(WebServerConfiguration.SCHEMA);

        String name = element.get("name", System.getProperty("com.exametrika.hostName"));
        long port = element.get("port");
        Long unsecuredPort = element.get("unsecuredPort", null);
        String bindAddress = element.get("bindAddress", null);
        boolean secured = element.get("secured");
        String keyStorePath = removeSchema((String) element.get("keyStorePath", null));
        String keyStorePassword = element.get("keyStorePassword", null);
        WebServerConfiguration configuration = new WebServerConfiguration(name, (int) port, unsecuredPort != null ? unsecuredPort.intValue() : null,
                bindAddress, secured, keyStorePath, keyStorePassword);

        loadContext.setConfiguration(configuration);
    }

    private String removeSchema(String path) {
        if (path == null)
            return null;

        String schema = "file:";
        if (path.startsWith(schema))
            return path.substring(schema.length());
        else
            return path;
    }
}
