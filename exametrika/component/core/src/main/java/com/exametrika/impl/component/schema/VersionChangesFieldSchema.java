/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.config.schema.VersionChangesFieldSchemaConfiguration;
import com.exametrika.impl.component.fields.VersionChangesField;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link VersionChangesFieldSchema} is a version changes field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VersionChangesFieldSchema extends StructuredBlobFieldSchema {
    public VersionChangesFieldSchema(VersionChangesFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public IFieldObject createField(IField field) {
        return new VersionChangesField((ISimpleField) field);
    }
}
