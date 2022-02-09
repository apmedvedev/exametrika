/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link IFieldConverter} represents a field converter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFieldConverter {
    /**
     * Converts old field value to new field value.
     *
     * @param oldField        old field
     * @param newField        new field
     * @param migrationSchema migration schema
     */
    void convert(IField oldField, IField newField, IFieldMigrationSchema migrationSchema);
}
