/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.objectdb.fields.ComplexField;

/**
 * The {@link ComplexFieldSchemaConfiguration} represents a configuration of schema of complex field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComplexFieldSchemaConfiguration extends FieldSchemaConfiguration {
    public ComplexFieldSchemaConfiguration(String name, String alias, String description,
                                           int initialSize, int cacheSize) {
        super(name, alias, description, initialSize + ComplexField.FIELD_HEADER_SIZE,
                cacheSize + Memory.getShallowSize(ComplexField.class));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComplexFieldSchemaConfiguration))
            return false;

        ComplexFieldSchemaConfiguration configuration = (ComplexFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ComplexFieldSchemaConfiguration))
            return false;

        ComplexFieldSchemaConfiguration configuration = (ComplexFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
