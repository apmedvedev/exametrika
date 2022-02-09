/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.schema;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;


/**
 * The {@link IActionSchema} represents a schema of component action.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IActionSchema {
    /**
     * Returns action name.
     *
     * @return action name
     */
    String getName();

    /**
     * Action schema configuration.
     *
     * @return schema configuration
     */
    ActionSchemaConfiguration getConfiguration();

    /**
     * Returns execute permission.
     *
     * @return execute permission
     */
    IPermission getExecutePermission();
}
