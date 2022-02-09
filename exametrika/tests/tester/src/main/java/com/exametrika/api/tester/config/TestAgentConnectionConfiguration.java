/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestAgentConnectionConfiguration} is a configuration for test agent connection.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentConnectionConfiguration extends Configuration {
    private final String name;
    private final String host;
    private final int port;
    private final Map<String, String> properties;

    public TestAgentConnectionConfiguration(String name, String host, int port, Map<String, String> properties) {
        Assert.notNull(name);
        Assert.notNull(host);
        Assert.isTrue(port >= 0 && port <= 65535);
        Assert.notNull(properties);

        this.name = name;
        this.host = host;
        this.port = port;
        this.properties = Immutables.wrap(properties);
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestAgentConnectionConfiguration))
            return false;

        TestAgentConnectionConfiguration configuration = (TestAgentConnectionConfiguration) o;
        return name.equals(configuration.name) && host.equals(configuration.host) && port == configuration.port &&
                properties.equals(configuration.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, host, port, properties);
    }

    @Override
    public String toString() {
        return name + "(" + host + ":" + port + properties + ")";
    }
}
