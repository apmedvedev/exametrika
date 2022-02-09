/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.config.schema.JsonBlobFieldSchemaConfiguration;


/**
 * The {@link IJsonBlobFieldSchema} represents a schema for json blob field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJsonBlobFieldSchema extends IFieldSchema {
    @Override
    JsonBlobFieldSchemaConfiguration getConfiguration();

    /**
     * Returns full text document schema.
     *
     * @return full text document schema or null if blob does not have full text index
     */
    IDocumentSchema getDocumentSchema();
}
