/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.config.schema.ISchemaLoadContext;


/**
 * The {@link SchemaLoadContext} is a helper class that is used to load {@link ModuleSchemaConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SchemaLoadContext implements ISchemaLoadContext, IContextFactory, IConfigurationFactory {
    private ModuleSchemaConfiguration currentModule;
    protected Map<String, ModuleSchemaConfiguration> modules = new LinkedHashMap<String, ModuleSchemaConfiguration>();
    private Map<String, Pair<String, Boolean>> topLevelDatabaseElements;

    public SchemaLoadContext(Map<String, Pair<String, Boolean>> topLevelDatabaseElements) {
        Assert.notNull(topLevelDatabaseElements);

        this.topLevelDatabaseElements = Immutables.wrap(topLevelDatabaseElements);
    }

    public void setCurrentModule(ModuleSchemaConfiguration module) {
        currentModule = module;
    }

    public Map<String, Pair<String, Boolean>> getTopLevelDatabaseElements() {
        return topLevelDatabaseElements;
    }

    @Override
    public ModuleSchemaConfiguration getCurrentModule() {
        return currentModule;
    }

    @Override
    public Map<String, ModuleSchemaConfiguration> getModules() {
        return Immutables.wrap(modules);
    }

    @Override
    public void addModule(ModuleSchemaConfiguration module) {
        Assert.notNull(module);

        modules.put(module.getName(), module);
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        for (ModuleSchemaConfiguration module : modules.values())
            module.getSchema().freeze();

        return Immutables.wrap(new LinkedHashSet(modules.values()));
    }

    @Override
    public IConfigurationFactory createContext() {
        return this;
    }
}
