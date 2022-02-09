/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Pair;


/**
 * The {@link InitialSchemaLoadContext} is a helper class that is used to load {@link ModuleSchemaConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class InitialSchemaLoadContext extends SchemaLoadContext {
    private String name;
    private String alias;
    private String description;
    private String locale;
    private String timeZone;

    public InitialSchemaLoadContext(Map<String, Pair<String, Boolean>> topLevelDatabaseElements) {
        super(topLevelDatabaseElements);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public ModularDatabaseSchemaConfiguration createConfiguration(ILoadContext context) {
        Set<ModuleSchemaConfiguration> modules = (Set<ModuleSchemaConfiguration>) super.createConfiguration(context);

        return new ModularDatabaseSchemaConfiguration(name, alias, description, modules, timeZone, locale);
    }
}
