/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.schema;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;


/**
 * The {@link ISelectorSchema} represents a schema of component selector.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISelectorSchema {
    /**
     * Returns selector name.
     *
     * @return selector name
     */
    String getName();

    /**
     * Selector schema configuration.
     *
     * @return schema configuration
     */
    SelectorSchemaConfiguration getConfiguration();

    /**
     * Returns execute permission.
     *
     * @return execute permission
     */
    IPermission getExecutePermission();
}
