/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;

import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link SchemaConfiguration} represents an abstract configuration of schema of database element.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SchemaConfiguration extends Configuration {
    private final String name;
    private final String alias;
    private final String description;

    public SchemaConfiguration(String name, String alias, String description) {
        Assert.notNull(name);
        Assert.notNull(alias);

        this.name = name;
        this.alias = alias;
        this.description = description != null ? description : "";
    }

    public final String getName() {
        return name;
    }

    public final String getAlias() {
        return alias;
    }

    public final String getDescription() {
        return description;
    }

    public <T extends SchemaConfiguration> T combine(T schema) {
        Assert.checkState(equals(schema));
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SchemaConfiguration))
            return false;

        SchemaConfiguration configuration = (SchemaConfiguration) o;
        return name.equals(configuration.name) && alias.equals(configuration.alias) && description.equals(configuration.description);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, alias, description);
    }

    @Override
    public String toString() {
        return alias;
    }

    protected final <T extends SchemaConfiguration> T combine(T schema, Map<String, ? extends SchemaConfiguration> schemas) {
        SchemaConfiguration combined = schemas.remove(schema.getName());
        if (combined != null)
            return (T) schema.combine(combined);
        else
            return schema;
    }

    protected final <T extends SchemaConfiguration> T combine(T value1, T value2) {
        if (value1 == null)
            return value2;
        else if (value2 == null)
            return value1;
        else
            return value1.combine(value2);
    }

    protected final <T> T combine(T value1, T value2) {
        if (value1 == null)
            return value2;
        else if (value2 == null)
            return value1;
        else {
            Assert.isTrue(value1.equals(value2));
            return value1;
        }
    }

    protected final String combine(String value1, String value2) {
        if (value1 == null || value1.isEmpty())
            return value2;
        else if (value2 == null || value2.isEmpty())
            return value1;
        else {
            Assert.isTrue(value1.equals(value2));
            return value1;
        }
    }

    protected final int combine(int value1, int value2) {
        if (value1 == 0)
            return value2;
        else if (value2 == 0)
            return value1;
        else {
            Assert.isTrue(value1 == value2);
            return value1;
        }
    }
}
