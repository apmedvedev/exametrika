/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;


/**
 * The {@link IDatabaseFactory} represents a factory of exa database.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDatabaseFactory {
    /**
     * Database runtime parameters.
     */
    class Parameters {
        /**
         * External compartment. Can be null if internal compartment is used.
         */
        public ICompartment compartment;
        /**
         * Is database owner of external compartment?
         */
        public boolean compartmentOwner;
        /**
         * External resource allocator. Can be null if external resource allocator is not used.
         */
        public IResourceAllocator resourceAllocator;
        /**
         * Timer operation. Can be null.
         */
        public IOperation timerOperation;
        /**
         * Initial schema provider. Can be null.
         */
        public IInitialSchemaProvider initialSchemaProvider;
        /**
         * Additional wiring parameters.
         */
        public final Map<String, Object> parameters = new HashMap<String, Object>();
    }

    /**
     * Creates database.
     *
     * @param parameters    runtime parameters. Can be null if not used
     * @param configuration database configuration
     * @return database
     */
    IDatabase createDatabase(Parameters parameters, DatabaseConfiguration configuration);
}
