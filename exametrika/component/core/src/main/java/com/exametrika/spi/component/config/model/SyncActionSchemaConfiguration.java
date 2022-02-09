/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;


/**
 * The {@link SyncActionSchemaConfiguration} represents a configuration of schema of component asynchronous action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SyncActionSchemaConfiguration extends ActionSchemaConfiguration {
    public SyncActionSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
