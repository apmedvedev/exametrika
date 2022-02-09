/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.List;

import com.exametrika.common.utils.ILifecycle;
import com.exametrika.spi.component.config.AlertChannelConfiguration;


/**
 * The {@link IAlertChannel} represents an alert channel.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAlertChannel extends ILifecycle {
    /**
     * Returns channel name.
     *
     * @return channel name
     */
    String getName();

    /**
     * Returns channel configuration.
     *
     * @return channel configuration
     */
    AlertChannelConfiguration getConfiguration();

    /**
     * Sets channel configuration.
     *
     * @param configuration channel configuration or null if default configuration is used
     */
    void setConfiguration(AlertChannelConfiguration configuration);

    /**
     * Sends specified messages to channel.
     *
     * @param messages messages to send
     */
    void send(List<AlertMessage> messages);
}
