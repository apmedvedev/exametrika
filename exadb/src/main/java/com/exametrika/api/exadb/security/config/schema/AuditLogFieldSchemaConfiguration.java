/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import java.util.Collections;

import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.security.schema.AuditLogFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link AuditLogFieldSchemaConfiguration} represents a configuration of schema of audit log field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AuditLogFieldSchemaConfiguration extends StructuredBlobFieldSchemaConfiguration {
    public AuditLogFieldSchemaConfiguration(String name, String alias, String description, String blobStoreFieldName) {
        super(name, alias, description, null, blobStoreFieldName, true, false, null, false, 0,
                Collections.<StructuredBlobIndexSchemaConfiguration>emptyList(),
                false, null);
    }

    @Override
    public boolean hasSerializationRegistry() {
        return false;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new AuditLogFieldSchema(this, index, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AuditLogFieldSchemaConfiguration))
            return false;

        AuditLogFieldSchemaConfiguration configuration = (AuditLogFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof AuditLogFieldSchemaConfiguration))
            return false;

        AuditLogFieldSchemaConfiguration configuration = (AuditLogFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
