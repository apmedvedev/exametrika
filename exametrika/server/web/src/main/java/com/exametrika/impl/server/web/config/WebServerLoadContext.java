/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web.config;

import com.exametrika.api.server.web.config.WebServerConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;


/**
 * The {@link WebServerLoadContext} is a helper class that is used to load {@link WebServerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class WebServerLoadContext implements IContextFactory, IConfigurationFactory {
    private WebServerConfiguration configuration = new WebServerConfiguration(System.getProperty("com.exametrika.hostName"),
            8080, null, null, false, null, null);

    public void setConfiguration(WebServerConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        return configuration;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new WebServerLoadContext();
    }
}
