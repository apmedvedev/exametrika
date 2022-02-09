/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.objectdb.fields.BodyField;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link BodyFieldSchema} is a body field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BodyFieldSchema extends ComplexFieldSchema implements IFieldSchema {
    private final ISerializationRegistry serializationRegistry;

    public BodyFieldSchema(BodyFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        serializationRegistry = Serializers.createRegistry();
    }

    public ISerializationRegistry getSerializationRegistry() {
        return serializationRegistry;
    }

    @Override
    public BodyFieldSchemaConfiguration getConfiguration() {
        return (BodyFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new BodyField((IComplexField) field);
    }
}
