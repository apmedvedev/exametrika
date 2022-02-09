/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.schema;

import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link IFieldSchema} represents a schema for indexed field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    FieldSchemaConfiguration getConfiguration();

    /**
     * Returns schema of index document.
     *
     * @return schema of index document
     */
    IDocumentSchema getDocument();

    /**
     * Returns index of schema in node's field schemas.
     *
     * @return index of schema in node's field schemas
     */
    int getIndex();

    /**
     * Is field sortable?
     *
     * @return true if field is sortable
     */
    boolean isSortable();

    /**
     * Creates an index field.
     *
     * @param value field value
     * @return index field
     */
    IField createField(Object value);
}
