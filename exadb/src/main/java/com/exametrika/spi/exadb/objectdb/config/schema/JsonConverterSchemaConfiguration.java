/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.json.schema.IJsonConverter;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JsonConverterSchemaConfiguration} represents a configuration of JSON converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class JsonConverterSchemaConfiguration extends Configuration {
    private final String name;

    public JsonConverterSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract IJsonConverter createConverter();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonConverterSchemaConfiguration))
            return false;

        JsonConverterSchemaConfiguration configuration = (JsonConverterSchemaConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
