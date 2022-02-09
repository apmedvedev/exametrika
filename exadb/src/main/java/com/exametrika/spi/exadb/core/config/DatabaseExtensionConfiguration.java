/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DatabaseExtensionConfiguration} represents a configuration of database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class DatabaseExtensionConfiguration extends Configuration {
    private final String name;

    public DatabaseExtensionConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isCompatible(DatabaseExtensionConfiguration configuration) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DatabaseExtensionConfiguration))
            return false;

        DatabaseExtensionConfiguration configuration = (DatabaseExtensionConfiguration) o;
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
