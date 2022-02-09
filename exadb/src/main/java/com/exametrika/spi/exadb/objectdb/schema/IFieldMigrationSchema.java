/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link IFieldMigrationSchema} represents a schema of migration old field to new one.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldMigrationSchema {
    /**
     * Returns node migration schema.
     *
     * @return node migration schema
     */
    INodeMigrationSchema getNode();

    /**
     * Returns old field schema.
     *
     * @return old field schema
     */
    IFieldSchema getOldSchema();

    /**
     * Returns new field schema.
     *
     * @return new field scheman
     */
    IFieldSchema getNewSchema();

    /**
     * Returns field converter.
     *
     * @return field converter
     */
    IFieldConverter getConverter();
}
