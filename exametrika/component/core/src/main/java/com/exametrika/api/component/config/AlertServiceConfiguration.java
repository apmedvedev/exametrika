/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config;

import java.util.Collections;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.AlertChannelConfiguration;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;

/**
 * The {@link AlertServiceConfiguration} represents a configuration of alert service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertServiceConfiguration extends DomainServiceConfiguration {
    public static final String SCHEMA = "com.exametrika.component-1.0";
    public static final String NAME = "component.AlertService";

    private final long schedulePeriod;
    private final Map<String, AlertChannelConfiguration> channels;

    public AlertServiceConfiguration() {
        this(600000, Collections.<String, AlertChannelConfiguration>emptyMap());
    }

    public AlertServiceConfiguration(long schedulePeriod, Map<String, ? extends AlertChannelConfiguration> channels) {
        super(NAME);

        Assert.notNull(channels);

        this.schedulePeriod = schedulePeriod;
        this.channels = Immutables.wrap(channels);
    }

    public long getSchedulePeriod() {
        return schedulePeriod;
    }

    public Map<String, AlertChannelConfiguration> getChannels() {
        return channels;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertServiceConfiguration))
            return false;

        AlertServiceConfiguration configuration = (AlertServiceConfiguration) o;
        return schedulePeriod == configuration.schedulePeriod && channels.equals(configuration.channels);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(schedulePeriod, channels);
    }
}