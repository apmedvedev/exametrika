/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IUserInterfaceService} represents a user interface service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUserInterfaceService {
    String NAME = "component.UserInterfaceService";

    /**
     * Returns schema of user interface.
     *
     * @return schema of user interface
     */
    JsonObject getUserInterfaceSchema();

    /**
     * Processes update user interface request.
     *
     * @param request request
     * @return response
     */
    JsonObject processUpdateRequest(JsonObject request);
}
