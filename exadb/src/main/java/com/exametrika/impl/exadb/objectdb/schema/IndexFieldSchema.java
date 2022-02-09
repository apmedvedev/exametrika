/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.IndexFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.IndexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link IndexFieldSchema} is a index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IndexFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    public IndexFieldSchema(IndexFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public IndexFieldSchemaConfiguration getConfiguration() {
        return (IndexFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new IndexField((ISimpleField) field);
    }
}
