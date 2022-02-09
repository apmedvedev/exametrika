/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;

/**
 * The {@link ComponentJobOperationSchemaConfiguration} represents a configuration of schema of component job operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentJobOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    @Override
    public boolean isAsync() {
        return true;
    }
}
