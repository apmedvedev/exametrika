/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.config.TestAgentDiscoveryStrategyConfiguration;


/**
 * The {@link TestCoordinatorChannelConfiguration} is a configuration for test cordinator channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinatorChannelConfiguration extends Configuration {
    private final TestAgentDiscoveryStrategyConfiguration discoveryStrategy;
    private final String bindAddress;
    private final boolean secured;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestCoordinatorChannelConfiguration(TestAgentDiscoveryStrategyConfiguration discoveryStrategy, String bindAddress, boolean secured,
                                               String keyStorePath, String keyStorePassword) {
        Assert.notNull(discoveryStrategy);
        Assert.isTrue(!secured || (keyStorePath != null && keyStorePassword != null));

        this.discoveryStrategy = discoveryStrategy;
        this.bindAddress = bindAddress;
        this.secured = secured;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    public TestAgentDiscoveryStrategyConfiguration getDiscoveryStrategy() {
        return discoveryStrategy;
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
        if (!(o instanceof TestCoordinatorChannelConfiguration))
            return false;

        TestCoordinatorChannelConfiguration configuration = (TestCoordinatorChannelConfiguration) o;
        return discoveryStrategy.equals(configuration.discoveryStrategy) &&
                Objects.equals(bindAddress, configuration.bindAddress) && secured == configuration.secured &&
                Objects.equals(keyStorePath, configuration.keyStorePath) && Objects.equals(keyStorePassword, configuration.keyStorePassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(discoveryStrategy, bindAddress, secured, keyStorePath, keyStorePassword);
    }

    @Override
    public String toString() {
        return discoveryStrategy.toString();
    }
}
