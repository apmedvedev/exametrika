/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import java.util.Collections;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Version;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link ModuleSchemaConfiguration} is a module schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ModuleSchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.core-1.0";

    private final Version version;
    private final DatabaseSchemaConfiguration schema;
    private final Set<ModuleDependencySchemaConfiguration> dependencies;

    public ModuleSchemaConfiguration(String name, Version version, DomainSchemaConfiguration schema) {
        this(name, name, null, version, new DatabaseSchemaConfiguration(schema.getName(),
                com.exametrika.common.utils.Collections.asSet(schema)), Collections.<ModuleDependencySchemaConfiguration>emptySet());
    }

    public ModuleSchemaConfiguration(String name, Version version, Set<DomainSchemaConfiguration> schemas) {
        this(name, name, null, version, new DatabaseSchemaConfiguration(name,
                schemas), Collections.<ModuleDependencySchemaConfiguration>emptySet());
    }

    public ModuleSchemaConfiguration(String name, Version version, DatabaseSchemaConfiguration schema) {
        this(name, name, null, version, schema, Collections.<ModuleDependencySchemaConfiguration>emptySet());
    }

    public ModuleSchemaConfiguration(String name, Version version, DatabaseSchemaConfiguration schema,
                                     Set<ModuleDependencySchemaConfiguration> dependencies) {
        this(name, name, null, version, schema, dependencies);
    }

    public ModuleSchemaConfiguration(String name, String alias, String description, Version version, DatabaseSchemaConfiguration schema,
                                     Set<ModuleDependencySchemaConfiguration> dependencies) {
        super(name, alias, description);

        Assert.notNull(version);
        Assert.notNull(schema);
        Assert.notNull(dependencies);

        this.version = version;
        this.schema = schema;
        this.dependencies = Immutables.wrap(dependencies);
    }

    public Version getVersion() {
        return version;
    }

    public DatabaseSchemaConfiguration getSchema() {
        return schema;
    }

    public Set<ModuleDependencySchemaConfiguration> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ModuleSchemaConfiguration))
            return false;

        ModuleSchemaConfiguration configuration = (ModuleSchemaConfiguration) o;
        return super.equals(configuration) && version.equals(configuration.version) && schema.equals(configuration.schema) &&
                dependencies.equals(configuration.dependencies);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(version, schema, dependencies);
    }

    @Override
    public String toString() {
        return getAlias() + "-" + version;
    }
}
