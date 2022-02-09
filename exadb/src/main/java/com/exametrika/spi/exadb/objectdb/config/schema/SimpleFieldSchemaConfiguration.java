/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.objectdb.fields.SimpleField;


/**
 * The {@link SimpleFieldSchemaConfiguration} represents a configuration of schema of simple field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SimpleFieldSchemaConfiguration extends FieldSchemaConfiguration {
    public SimpleFieldSchemaConfiguration(String name, String alias, String description, int initialSize,
                                          int cacheSize) {
        super(name, alias, description, initialSize, cacheSize + Memory.getShallowSize(SimpleField.class));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleFieldSchemaConfiguration))
            return false;

        SimpleFieldSchemaConfiguration configuration = (SimpleFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof SimpleFieldSchemaConfiguration))
            return false;

        SimpleFieldSchemaConfiguration configuration = (SimpleFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
