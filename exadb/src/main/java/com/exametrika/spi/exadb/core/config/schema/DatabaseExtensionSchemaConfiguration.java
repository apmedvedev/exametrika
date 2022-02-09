/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;


/**
 * The {@link DatabaseExtensionSchemaConfiguration} represents a configuration of schema of database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DatabaseExtensionSchemaConfiguration extends SchemaConfiguration {
    public DatabaseExtensionSchemaConfiguration(String name) {
        this(name, name, null);
    }

    public DatabaseExtensionSchemaConfiguration(String name, String alias, String description) {
        super(name, alias, description);
    }

    public void freeze() {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DatabaseExtensionSchemaConfiguration))
            return false;

        DatabaseExtensionSchemaConfiguration configuration = (DatabaseExtensionSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
