/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;


/**
 * The {@link SimpleFieldSchema} is a simple field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SimpleFieldSchema extends FieldSchema implements IFieldSchema {
    public SimpleFieldSchema(SimpleFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public SimpleFieldSchemaConfiguration getConfiguration() {
        return (SimpleFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return (IFieldObject) field;
    }

    @Override
    public void validate(IField field) {
    }
}
