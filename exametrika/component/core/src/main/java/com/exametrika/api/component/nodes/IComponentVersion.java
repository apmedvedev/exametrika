/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link IComponentVersion} represents a component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComponentVersion {
    /**
     * Returns component schema configuration.
     *
     * @return component schema configuration
     */
    ComponentSchemaConfiguration getConfiguration();

    /**
     * Returns component title.
     *
     * @return component title
     */
    String getTitle();

    /**
     * Returns component description.
     *
     * @return component description
     */
    String getDescription();

    /**
     * Is component deleted?
     *
     * @return true if component marked as deleted
     */
    boolean isDeleted();

    /**
     * Returns version creation time.
     *
     * @return version creation time
     */
    long getTime();

    /**
     * Returns component options.
     *
     * @return component options
     */
    JsonObject getOptions();

    /**
     * Returns component properties.
     *
     * @return component properties
     */
    JsonObject getProperties();

    /**
     * Returns component.
     *
     * @return component
     */
    IComponent getComponent();

    /**
     * Returns previous version.
     *
     * @return previous version or null if this version is first
     */
    IComponentVersion getPreviousVersion();

    /**
     * Returns aggregation groups.
     *
     * @return aggregation groups
     */
    Iterable<IGroupComponent> getGroups();
}
