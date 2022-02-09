/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;


/**
 * The {@link SchemaOperation} is a default implementation of schema operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SchemaOperation implements ISchemaOperation {
    @Override
    public int getSize() {
        return 1;
    }
}
