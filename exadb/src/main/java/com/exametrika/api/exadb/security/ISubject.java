/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import java.util.List;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link ISubject} represents a security subject.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISubject {
    /**
     * Returns name.
     *
     * @return name
     */
    String getName();

    /**
     * Returns description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Sets description.
     *
     * @param description description
     */
    void setDescription(String description);

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
     * Returns roles.
     *
     * @return roles
     */
    Iterable<IRole> getRoles();

    /**
     * Finds role by name.
     *
     * @param name role name
     * @return role or null if role is not found
     */
    IRole findRole(String name);

    /**
     * Adds role.
     *
     * @param name role name
     * @return role
     */
    IRole addRole(String name);

    /**
     * Removes role.
     *
     * @param name role name
     */
    void removeRole(String name);

    /**
     * Removes all roles.
     */
    void removeAllRoles();

    /**
     * Returns security labels.
     *
     * @return security labels
     */
    List<String> getLabels();

    /**
     * Sets security labels.
     *
     * @param labels security labels
     */
    void setLabels(List<String> labels);

    /**
     * Deletes subject.
     */
    void delete();
}
