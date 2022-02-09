/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import java.util.List;

import com.exametrika.spi.component.IAlert;


/**
 * The {@link IIncident} represents an incident.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIncident {
    /**
     * Returns unique identifier of incident.
     *
     * @return unique identifier of incident
     */
    int getIncidentId();

    /**
     * Returns alert.
     *
     * @return alert
     */
    IAlert getAlert();

    /**
     * Returns name.
     *
     * @return name
     */
    String getName();

    /**
     * Returns incident message.
     *
     * @return incident message
     */
    String getMessage();

    /**
     * Returns creation time of incident.
     *
     * @return creation time of incident
     */
    long getCreationTime();

    /**
     * Returns component.
     *
     * @return component
     */
    IComponent getComponent();

    /**
     * Returns component state.
     *
     * @return component state
     */
    String getState();

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
     * Deletes incident.
     *
     * @param resolved if true incident is resolved
     */
    void delete(boolean resolved);
}
