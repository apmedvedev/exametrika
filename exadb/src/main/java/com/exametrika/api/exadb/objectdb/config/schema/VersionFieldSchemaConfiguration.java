/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.objectdb.fields.VersionField;
import com.exametrika.impl.exadb.objectdb.fields.VersionFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.VersionFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link VersionFieldSchemaConfiguration} represents a configuration of schema of version field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class VersionFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    public VersionFieldSchemaConfiguration(String name) {
        this(name, name, null);
    }

    public VersionFieldSchemaConfiguration(String name, String alias, String description) {
        super(name, alias, description, 8, Memory.getShallowSize(VersionField.class));
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new VersionFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof VersionFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new VersionFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VersionFieldSchemaConfiguration))
            return false;

        VersionFieldSchemaConfiguration configuration = (VersionFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof VersionFieldSchemaConfiguration))
            return false;

        VersionFieldSchemaConfiguration configuration = (VersionFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
