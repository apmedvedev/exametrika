/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;


/**
 * The {@link IDocumentSchema} represents a schema for indexed document.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDocumentSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    DocumentSchemaConfiguration getConfiguration();

    /**
     * Returns index analyzer.
     *
     * @return index analyzer
     */
    IAnalyzer getAnalyzer();

    /**
     * Returns list of field schemas.
     *
     * @return list of field schemas
     */
    List<IFieldSchema> getFields();

    /**
     * Finds field schema by name.
     *
     * @param fieldName name of field
     * @return field schema or null if field is not found
     */
    IFieldSchema findField(String fieldName);

    /**
     * Creates index document.
     *
     * @param values list of document field values
     * @return index document
     */
    IDocument createDocument(Object... values);

    /**
     * Creates index document.
     *
     * @param values list of document field values
     * @return index document
     */
    IDocument createDocument(List<? extends Object> values);

    /**
     * Creates index document.
     *
     * @param context user defined context or null if context is not used
     * @param values  list of document field values
     * @return index document
     */
    IDocument createDocument(Object context, List<? extends Object> values);
}
