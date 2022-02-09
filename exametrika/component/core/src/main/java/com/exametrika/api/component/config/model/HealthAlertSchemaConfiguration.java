/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.alerts.HealthAlert;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link HealthAlertSchemaConfiguration} is an health alert schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthAlertSchemaConfiguration extends AlertSchemaConfiguration {
    private final State stateThreshold;

    public HealthAlertSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels,
                                          List<String> tags, boolean enabled, State stateThreshold) {
        super(name, description, channels, tags, enabled);

        Assert.notNull(stateThreshold);

        this.stateThreshold = stateThreshold;
    }

    public State getStateThreshold() {
        return stateThreshold;
    }

    @Override
    public IAlert createAlert(IDatabaseContext context) {
        return new HealthAlert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HealthAlertSchemaConfiguration))
            return false;

        HealthAlertSchemaConfiguration configuration = (HealthAlertSchemaConfiguration) o;
        return super.equals(configuration) && stateThreshold == configuration.stateThreshold;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(stateThreshold);
    }
}
