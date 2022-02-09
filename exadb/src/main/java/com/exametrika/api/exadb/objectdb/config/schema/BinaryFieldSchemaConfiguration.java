/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.BinaryField;
import com.exametrika.impl.exadb.objectdb.fields.BinaryFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.BinaryFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link BinaryFieldSchemaConfiguration} represents a configuration of schema of binary blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BinaryFieldSchemaConfiguration extends BlobFieldSchemaConfiguration {
    private final boolean compressed;

    public BinaryFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName) {
        this(name, name, null, blobStoreNodeType, blobStoreFieldName, false, true);
    }

    public BinaryFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                          boolean required, boolean compressed) {
        super(name, alias, description, blobStoreNodeType, blobStoreFieldName, required, 0, Memory.getShallowSize(BinaryField.class));

        this.compressed = compressed;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new BinaryFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof BinaryFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new BinaryFieldConverter();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BinaryFieldSchemaConfiguration))
            return false;

        BinaryFieldSchemaConfiguration configuration = (BinaryFieldSchemaConfiguration) o;
        return super.equals(configuration) && compressed == configuration.compressed;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BinaryFieldSchemaConfiguration))
            return false;

        BinaryFieldSchemaConfiguration configuration = (BinaryFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(compressed);
    }
}
