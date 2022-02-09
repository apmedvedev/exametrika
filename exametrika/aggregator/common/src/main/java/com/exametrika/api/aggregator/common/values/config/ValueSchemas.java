/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;


/**
 * The {@link ValueSchemas} is a utility class for building value schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ValueSchemas {
    public static ComponentValueSchemaBuilder component(String name) {
        return new ComponentValueSchemaBuilder(name);
    }

    private ValueSchemas() {
    }
}
