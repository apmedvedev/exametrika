/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestAgentChannelConfiguration} is a configuration for test agent channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentChannelConfiguration extends Configuration {
    private final int port;
    private final String bindAddress;
    private final boolean secured;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestAgentChannelConfiguration(int port, String bindAddress, boolean secured,
                                         String keyStorePath, String keyStorePassword) {
        Assert.isTrue(port >= 0 && port <= 65535);
        Assert.isTrue(!secured || (keyStorePath != null && keyStorePassword != null));

        this.port = port;
        this.bindAddress = bindAddress;
        this.secured = secured;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
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

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestAgentChannelConfiguration))
            return false;

        TestAgentChannelConfiguration configuration = (TestAgentChannelConfiguration) o;
        return port == configuration.port && Objects.equals(bindAddress, configuration.bindAddress) && secured == configuration.secured &&
                Objects.equals(keyStorePath, configuration.keyStorePath) && Objects.equals(keyStorePassword, configuration.keyStorePassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(port, bindAddress, secured, keyStorePath, keyStorePassword);
    }

    @Override
    public String toString() {
        return Integer.toString(port);
    }
}
