/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link IObjectMigrator} represents a data migrator which allows customize process of data migration on schema change.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectMigrator extends IDataMigrator {
    /**
     * Is specified field migration supported by data migrator? If field migration is not supported by this data migrator
     * standard data migration is used.
     *
     * @param oldSchema old field schema
     * @param newSchema new field schema
     * @return true if specified field migration is supported by data migrator
     */
    boolean supports(IFieldSchema oldSchema, IFieldSchema newSchema);

    /**
     * Are field configurations compatible?
     *
     * @param oldSchema old field schema
     * @param newSchema new field schema
     * @return true if field configurations are compatible
     */
    boolean isCompatible(IFieldSchema oldSchema, IFieldSchema newSchema);

    /**
     * Creates field converter for specified compatible field configurations.
     *
     * @param oldSchema old field schema
     * @param newSchema new field schema
     * @return field converter
     */
    IFieldConverter createConverter(IFieldSchema oldSchema, IFieldSchema newSchema);
}
