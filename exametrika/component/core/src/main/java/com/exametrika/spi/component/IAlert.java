/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.List;

import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.schema.IAlertChannelSchema;


/**
 * The {@link IAlert} represents a component alert.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAlert {
    /**
     * Returns alert schema configuration.
     *
     * @return alert schema configuration
     */
    AlertSchemaConfiguration getConfiguration();

    /**
     * Returns schemas of alert channels.
     *
     * @return schemas of alert channels
     */
    List<IAlertChannelSchema> getChannels();
}
