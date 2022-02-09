/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import java.util.List;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IBehaviorType} represents a behavior type node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBehaviorType {
    /**
     * Returns type identifier.
     *
     * @return type identifier
     */
    int getTypeId();

    /**
     * Returns name.
     *
     * @return name
     */
    String getName();

    /**
     * Sets name.
     *
     * @param value name
     */
    void setName(String value);

    /**
     * Returns metadata.
     *
     * @return metadata
     */
    JsonObject getMetadata();

    /**
     * Sets metadata.
     *
     * @param value description
     */
    void setMetadata(JsonObject value);

    /**
     * Returns tags.
     *
     * @return tags or null if tags are not set
     */
    List<String> getTags();

    /**
     * Sets tags.
     *
     * @param tags tags or null if tags are cleared
     */
    void setTags(List<String> tags);

    /**
     * Deletes behavior type.
     */
    void delete();
}
