/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;


/**
 * The {@link IAgentSchemaUpdater} represents an agent schema updater.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAgentSchemaUpdater {
    String NAME = "agentSchemaUpdater";

    /**
     * Called when new component model schema has been set.
     *
     * @param newSchema new component model schema configuration
     */
    void onSchemaChanged(ComponentModelSchemaConfiguration newSchema);
}
