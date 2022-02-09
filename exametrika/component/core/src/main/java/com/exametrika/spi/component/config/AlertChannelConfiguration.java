/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;

/**
 * The {@link AlertChannelConfiguration} represents a configuration of alert channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AlertChannelConfiguration extends Configuration {
    private final String name;

    public AlertChannelConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertChannelConfiguration))
            return false;

        AlertChannelConfiguration configuration = (AlertChannelConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
