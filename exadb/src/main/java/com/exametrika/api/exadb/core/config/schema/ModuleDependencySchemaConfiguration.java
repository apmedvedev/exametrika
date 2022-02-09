/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Version;


/**
 * The {@link ModuleDependencySchemaConfiguration} is a module dependency schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ModuleDependencySchemaConfiguration extends Configuration {
    private final String name;
    private final Version version;

    public ModuleDependencySchemaConfiguration(String name, Version version) {
        Assert.notNull(name);
        Assert.notNull(version);

        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ModuleDependencySchemaConfiguration))
            return false;

        ModuleDependencySchemaConfiguration configuration = (ModuleDependencySchemaConfiguration) o;
        return name.equals(configuration.name) && version.equals(configuration.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, version);
    }

    @Override
    public String toString() {
        return name + "-" + version;
    }
}
