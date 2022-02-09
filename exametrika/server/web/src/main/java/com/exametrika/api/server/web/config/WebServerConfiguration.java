/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.server.web.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link WebServerConfiguration} is a configuration for web server.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class WebServerConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.server.web-1.0";
    private final String name;
    private final int port;
    private final String bindAddress;
    private final boolean secured;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final Integer unsecuredPort;

    public WebServerConfiguration(String name, int port, Integer unsecuredPort, String bindAddress, boolean secured,
                                  String keyStorePath, String keyStorePassword) {
        Assert.notNull(name);
        Assert.isTrue(port >= 0 && port <= 65535);
        Assert.isTrue(!secured || (keyStorePath != null && keyStorePassword != null));

        this.name = name;
        this.port = port;
        this.unsecuredPort = unsecuredPort;
        this.bindAddress = bindAddress;
        this.secured = secured;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public Integer getUnsecuredPort() {
        return unsecuredPort;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public boolean isSecured() {
        return secured;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof WebServerConfiguration))
            return false;

        WebServerConfiguration configuration = (WebServerConfiguration) o;
        return name.equals(configuration.name) && port == configuration.port && Objects.equals(unsecuredPort, configuration.unsecuredPort) &&
                Objects.equals(bindAddress, configuration.bindAddress) && secured == configuration.secured &&
                Objects.equals(keyStorePath, configuration.keyStorePath) && Objects.equals(keyStorePassword, configuration.keyStorePassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, port, unsecuredPort, bindAddress, secured, keyStorePath, keyStorePassword);
    }

    @Override
    public String toString() {
        return name + ":" + Integer.toString(port);
    }
}
