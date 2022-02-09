/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;

import java.util.Map;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;


/**
 * The {@link ISchemaLoadContext} represents a module schema load context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchemaLoadContext {
    /**
     * Returns currently loaded module.
     *
     * @return currently loaded module
     */
    ModuleSchemaConfiguration getCurrentModule();

    /**
     * Returns all loaded modules.
     *
     * @return all loaded modules
     */
    Map<String, ModuleSchemaConfiguration> getModules();

    /**
     * Adds module.
     *
     * @param module schema
     */
    void addModule(ModuleSchemaConfiguration module);
}
