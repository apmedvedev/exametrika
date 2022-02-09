/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import java.util.List;

import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AlertSchemaConfiguration} represents a configuration of schema of component alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AlertSchemaConfiguration extends Configuration {
    private final String name;
    private final String description;
    private final List<AlertChannelSchemaConfiguration> channels;
    private final List<String> tags;
    private final boolean enabled;

    public AlertSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels, List<String> tags, boolean enabled) {
        Assert.notNull(name);
        Assert.notNull(channels);

        this.name = name;
        this.description = description;
        this.channels = Immutables.wrap(channels);
        this.tags = Immutables.wrap(Collections.notNull(tags));
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<AlertChannelSchemaConfiguration> getChannels() {
        return channels;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract IAlert createAlert(IDatabaseContext context);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertSchemaConfiguration))
            return false;

        AlertSchemaConfiguration configuration = (AlertSchemaConfiguration) o;
        return name.equals(configuration.name) && Objects.equals(description, configuration.description) &&
                channels.equals(configuration.channels) && tags.equals(configuration.tags) &&
                enabled == configuration.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, channels, tags, enabled);
    }

    @Override
    public String toString() {
        return name;
    }
}
