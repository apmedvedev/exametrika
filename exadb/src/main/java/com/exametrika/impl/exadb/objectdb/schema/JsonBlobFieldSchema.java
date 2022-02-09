/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.config.schema.JsonBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IJsonBlobFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.JsonBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link JsonBlobFieldSchema} is a json blob field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonBlobFieldSchema extends StructuredBlobFieldSchema implements IJsonBlobFieldSchema {
    private final IDocumentSchema documentSchema;

    public JsonBlobFieldSchema(JsonBlobFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        if (configuration.getDocumentSchemaFactory() != null)
            documentSchema = configuration.getDocumentSchemaFactory().createSchema().createSchema();
        else
            documentSchema = null;
    }

    @Override
    public JsonBlobFieldSchemaConfiguration getConfiguration() {
        return (JsonBlobFieldSchemaConfiguration) configuration;
    }

    @Override
    public IDocumentSchema getDocumentSchema() {
        return documentSchema;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new JsonBlobField((ISimpleField) field);
    }
}
