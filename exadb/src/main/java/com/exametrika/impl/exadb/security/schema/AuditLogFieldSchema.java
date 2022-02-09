/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.schema;

import com.exametrika.api.exadb.security.config.schema.AuditLogFieldSchemaConfiguration;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.impl.exadb.security.fields.AuditLogField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link AuditLogFieldSchema} is a audit log field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AuditLogFieldSchema extends StructuredBlobFieldSchema {
    public AuditLogFieldSchema(AuditLogFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public IFieldObject createField(IField field) {
        return new AuditLogField((ISimpleField) field);
    }
}
