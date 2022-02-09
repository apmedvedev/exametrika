/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.VersionFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.VersionField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link VersionFieldSchema} is a version field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VersionFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    public VersionFieldSchema(VersionFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public VersionFieldSchemaConfiguration getConfiguration() {
        return (VersionFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new VersionField((ISimpleField) field);
    }
}
