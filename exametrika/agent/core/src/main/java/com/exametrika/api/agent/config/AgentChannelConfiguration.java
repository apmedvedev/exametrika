/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.agent.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AgentChannelConfiguration} is a configuration for agent channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentChannelConfiguration extends Configuration {
    private final String serverHost;
    private final int serverPort;
    private final String bindAddress;
    private final boolean secured;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final Integer maxRate;
    private final TransportConfiguration transport;

    public AgentChannelConfiguration(String serverHost, int serverPort, String bindAddress, boolean secured,
                                     String keyStorePath, String keyStorePassword, Integer maxRate, TransportConfiguration transport) {
        Assert.notNull(serverHost);
        Assert.isTrue(serverPort >= 0 && serverPort <= 65535);
        Assert.isTrue(!secured || (keyStorePath != null && keyStorePassword != null));

        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.bindAddress = bindAddress;
        this.secured = secured;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.maxRate = maxRate;
        this.transport = transport;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
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

    public Integer getMaxRate() {
        return maxRate;
    }

    public TransportConfiguration getTransport() {
        return transport;

    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AgentChannelConfiguration))
            return false;

        AgentChannelConfiguration configuration = (AgentChannelConfiguration) o;
        return serverHost.equals(configuration.serverHost) && serverPort == configuration.serverPort &&
                Objects.equals(bindAddress, configuration.bindAddress) && secured == configuration.secured &&
                Objects.equals(keyStorePath, configuration.keyStorePath) && Objects.equals(keyStorePassword, configuration.keyStorePassword) &&
                Objects.equals(maxRate, configuration.maxRate) && Objects.equals(transport, configuration.transport);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serverHost, serverPort, bindAddress, secured, keyStorePath, keyStorePassword, maxRate,
                transport);
    }

    @Override
    public String toString() {
        return serverHost + ":" + serverPort;
    }
}
