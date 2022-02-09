/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JsonValidatorSchemaConfiguration} represents a configuration of JSON validator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class JsonValidatorSchemaConfiguration extends Configuration {
    private final String name;

    public JsonValidatorSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract IJsonValidator createValidator();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonValidatorSchemaConfiguration))
            return false;

        JsonValidatorSchemaConfiguration configuration = (JsonValidatorSchemaConfiguration) o;
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
