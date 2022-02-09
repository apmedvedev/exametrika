/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.BlobField;
import com.exametrika.impl.exadb.objectdb.fields.BlobFieldInitializer;
import com.exametrika.impl.exadb.objectdb.schema.BlobFieldSchema;


/**
 * The {@link BlobFieldSchemaConfiguration} represents a configuration of schema of blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class BlobFieldSchemaConfiguration extends SingleReferenceFieldSchemaConfiguration {
    private final String blobStoreNodeType;
    private final String blobStoreFieldName;

    public BlobFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName, boolean required) {
        this(name, name, null, blobStoreNodeType, blobStoreFieldName, required, 0, 0);
    }

    public BlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                        boolean required) {
        this(name, alias, description, blobStoreNodeType, blobStoreFieldName, required, 0, 0);
    }

    public BlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                        boolean required, int size, int cacheSize) {
        super(name, alias, description, null, null, required, false, false, null, 8 + size, cacheSize + Memory.getShallowSize(BlobField.class));

        Assert.notNull(blobStoreFieldName);

        this.blobStoreNodeType = blobStoreNodeType;
        this.blobStoreFieldName = blobStoreFieldName;
    }

    public final String getBlobStoreNodeType() {
        return blobStoreNodeType;
    }

    public final String getBlobStoreFieldName() {
        return blobStoreFieldName;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new BlobFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof BlobFieldSchemaConfiguration;
    }

    @Override
    public Object createInitializer() {
        return new BlobFieldInitializer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BlobFieldSchemaConfiguration))
            return false;

        BlobFieldSchemaConfiguration configuration = (BlobFieldSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(blobStoreNodeType, configuration.blobStoreNodeType) &&
                blobStoreFieldName.equals(configuration.blobStoreFieldName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BlobFieldSchemaConfiguration))
            return false;

        BlobFieldSchemaConfiguration configuration = (BlobFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && Objects.equals(blobStoreNodeType, configuration.blobStoreNodeType) &&
                blobStoreFieldName.equals(configuration.blobStoreFieldName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(blobStoreNodeType, blobStoreFieldName);
    }
}
