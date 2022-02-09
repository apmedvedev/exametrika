/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;


/**
 * The {@link IBlobFieldSchema} represents a schema for blob field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IBlobFieldSchema extends IFieldSchema {
    /**
     * Returns blob storage field schema this schema references to.
     *
     * @return referenced blob storage field schema
     */
    IFieldSchema getStore();
}
