/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link IPrimaryFieldConverter} represents a primary field converter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IPrimaryFieldConverter extends IFieldConverter {
    /**
     * Converts old field value to new field value.
     *
     * @param oldField        old field
     * @param migrationSchema migration schema
     * @return new field value
     */
    Object convert(IField oldField, IFieldMigrationSchema migrationSchema);
}
