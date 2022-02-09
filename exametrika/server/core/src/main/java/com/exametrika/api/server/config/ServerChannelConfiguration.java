/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.server.config;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ServerChannelConfiguration} is a configuration for server channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerChannelConfiguration extends Configuration {
    private final int port;
    private final String bindAddress;
    private final boolean secured;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final Integer maxTotalRate;
    private final TransportConfiguration transport;

    public ServerChannelConfiguration(int port, String bindAddress, boolean secured,
                                      String keyStorePath, String keyStorePassword, Integer maxTotalRate, TransportConfiguration transport) {
        Assert.isTrue(port >= 0 && port <= 65535);
        Assert.isTrue(!secured || (keyStorePath != null && keyStorePassword != null));

        this.port = port;
        this.bindAddress = bindAddress;
        this.secured = secured;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.maxTotalRate = maxTotalRate;
        this.transport = transport;
    }

    public int getPort() {
        return port;
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

    public Integer getMaxTotalRate() {
        return maxTotalRate;
    }

    public TransportConfiguration getTransport() {
        return transport;

    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ServerChannelConfiguration))
            return false;

        ServerChannelConfiguration configuration = (ServerChannelConfiguration) o;
        return port == configuration.port &&
                Objects.equals(bindAddress, configuration.bindAddress) && secured == configuration.secured &&
                Objects.equals(keyStorePath, configuration.keyStorePath) && Objects.equals(keyStorePassword, configuration.keyStorePassword) &&
                Objects.equals(maxTotalRate, configuration.maxTotalRate) && Objects.equals(transport, configuration.transport);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(port, bindAddress, secured, keyStorePath, keyStorePassword, maxTotalRate, transport);
    }

    @Override
    public String toString() {
        return Integer.toString(port);
    }
}
