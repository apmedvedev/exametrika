/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.server.config.ServerChannelConfiguration;
import com.exametrika.api.server.config.ServerConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ServerLoadContext} is a helper class that is used to load {@link ServerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ServerLoadContext implements IContextFactory, IConfigurationFactory {
    private String name;
    private ServerChannelConfiguration channel;
    private DatabaseConfiguration database;

    public void setName(String name) {
        this.name = name;
    }

    public void setChannel(ServerChannelConfiguration channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void setDatabase(DatabaseConfiguration database) {
        Assert.notNull(database);

        this.database = database;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        if (database != null)
            return new ServerConfiguration(name, channel, database);
        else
            return null;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new ServerLoadContext();
    }
}
