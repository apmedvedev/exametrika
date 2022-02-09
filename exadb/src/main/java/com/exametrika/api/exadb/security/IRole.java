/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IRole} represents a role.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRole {
    /**
     * Returns role name.
     *
     * @return role name
     */
    String getName();

    /**
     * Returns subject.
     *
     * @return subject
     */
    ISubject getSubject();

    /**
     * Returns metadata.
     *
     * @return metadata
     */
    JsonObject getMetadata();

    /**
     * Sets metadata.
     *
     * @param metadata metadata
     */
    void setMetadata(JsonObject metadata);

    /**
     * Deletes role.
     */
    void delete();
}
