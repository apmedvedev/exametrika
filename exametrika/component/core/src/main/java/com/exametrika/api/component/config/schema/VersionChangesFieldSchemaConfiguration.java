/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.Collections;

import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.component.schema.VersionChangesFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link VersionChangesFieldSchemaConfiguration} represents a configuration of schema of version changes field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionChangesFieldSchemaConfiguration extends StructuredBlobFieldSchemaConfiguration {
    public VersionChangesFieldSchemaConfiguration(String name, String alias, String description, String blobStoreFieldName) {
        super(name, alias, description, null, blobStoreFieldName, true, false, null, false, 0,
                Collections.<StructuredBlobIndexSchemaConfiguration>singletonList(new StructuredBlobIndexSchemaConfiguration(
                        "versionChangesIndex", 0, IndexType.BTREE, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG),
                        false, true, null)), false, new VersionChangesRecordIndexerSchemaConfiguration());
    }

    @Override
    public boolean hasSerializationRegistry() {
        return false;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new VersionChangesFieldSchema(this, index, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VersionChangesFieldSchemaConfiguration))
            return false;

        VersionChangesFieldSchemaConfiguration configuration = (VersionChangesFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof VersionChangesFieldSchemaConfiguration))
            return false;

        VersionChangesFieldSchemaConfiguration configuration = (VersionChangesFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
