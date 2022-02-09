/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;


import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.UuidField;
import com.exametrika.impl.exadb.objectdb.fields.UuidFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.UuidFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link UuidFieldSchemaConfiguration} represents a configuration of schema of UUID field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class UuidFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final boolean required;

    public UuidFieldSchemaConfiguration(String name, boolean required) {
        this(name, name, null, required);
    }

    public UuidFieldSchemaConfiguration(String name, String alias, String description, boolean required) {
        this(name, alias, description, required, 0);
    }

    public UuidFieldSchemaConfiguration(String name, String alias, String description, boolean required, int cacheSize) {
        super(name, alias, description, 16, cacheSize + Memory.getShallowSize(UuidField.class));

        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new UuidFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof UuidFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new UuidFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UuidFieldSchemaConfiguration))
            return false;

        UuidFieldSchemaConfiguration configuration = (UuidFieldSchemaConfiguration) o;
        return super.equals(configuration) && required == configuration.required;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof UuidFieldSchemaConfiguration))
            return false;

        UuidFieldSchemaConfiguration configuration = (UuidFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(required);
    }
}
