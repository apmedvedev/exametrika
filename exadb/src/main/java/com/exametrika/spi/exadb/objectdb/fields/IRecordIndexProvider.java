/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;

import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;


/**
 * The {@link IRecordIndexProvider} represents a index provider for indexing records.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRecordIndexProvider {
    /**
     * Creates document schema.
     *
     * @param schemaConfiguration schema configuration
     * @return document schema
     */
    IDocumentSchema createDocumentSchema(DocumentSchemaConfiguration schemaConfiguration);

    /**
     * Adds value to index.
     *
     * @param index numeric index of record index
     * @param key   index key
     * @param id    record identifier
     */
    void add(int index, Object key, long id);

    /**
     * Removes value from index.
     *
     * @param index numeric index of record index
     * @param key   index key
     */
    void remove(int index, Object key);

    /**
     * Adds document to fulltext index.
     *
     * @param schema document schema
     * @param id     record identifier
     * @param values document field values, starting from non-system fields
     */
    void add(IDocumentSchema schema, long id, Object... values);
}
