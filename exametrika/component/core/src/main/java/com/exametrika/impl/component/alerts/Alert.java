/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.component.schema.AlertChannelSchema;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.schema.IAlertChannelSchema;


/**
 * The {@link Alert} represents a alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Alert implements IAlert {
    private final AlertSchemaConfiguration configuration;
    private final List<IAlertChannelSchema> channels;

    public Alert(AlertSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;

        List<IAlertChannelSchema> channels = new ArrayList<IAlertChannelSchema>();
        for (AlertChannelSchemaConfiguration channel : configuration.getChannels())
            channels.add(new AlertChannelSchema(channel));

        this.channels = Immutables.wrap(channels);
    }

    @Override
    public AlertSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public List<IAlertChannelSchema> getChannels() {
        return channels;
    }
}
