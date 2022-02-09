/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.server.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ServerConfiguration} is a configuration for server.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.server-1.0";
    private final String name;
    private final ServerChannelConfiguration channel;
    private final DatabaseConfiguration database;

    public ServerConfiguration(String name, ServerChannelConfiguration channel, DatabaseConfiguration database) {
        Assert.notNull(name);
        Assert.notNull(channel);
        Assert.notNull(database);

        this.name = name;
        this.channel = channel;
        this.database = database;
    }

    public String getName() {
        return name;
    }

    public ServerChannelConfiguration getChannel() {
        return channel;
    }

    public DatabaseConfiguration getDatabase() {
        return database;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ServerConfiguration))
            return false;

        ServerConfiguration configuration = (ServerConfiguration) o;
        return name.equals(configuration.name) && channel.equals(configuration.channel) && database.equals(configuration.database);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, channel, database);
    }

    @Override
    public String toString() {
        return name;
    }
}
