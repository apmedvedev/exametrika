/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.TextField;
import com.exametrika.impl.exadb.objectdb.fields.TextFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.TextFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link TextFieldSchemaConfiguration} represents a configuration of schema of text blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TextFieldSchemaConfiguration extends BlobFieldSchemaConfiguration {
    private final boolean compressed;

    public TextFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName) {
        this(name, name, null, blobStoreNodeType, blobStoreFieldName, false, true);
    }

    public TextFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                        boolean required, boolean compressed) {
        super(name, alias, description, blobStoreNodeType, blobStoreFieldName, required, 0, Memory.getShallowSize(TextField.class));

        this.compressed = compressed;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new TextFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof TextFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new TextFieldConverter();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TextFieldSchemaConfiguration))
            return false;

        TextFieldSchemaConfiguration configuration = (TextFieldSchemaConfiguration) o;
        return super.equals(configuration) && compressed == configuration.compressed;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof TextFieldSchemaConfiguration))
            return false;

        TextFieldSchemaConfiguration configuration = (TextFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(compressed);
    }
}
