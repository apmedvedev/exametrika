/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.property.MapPropertyResolver;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ModuleSchemaLoader} is a loader of schemas of database modules.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ModuleSchemaLoader {
    private final Map<String, String> properties;
    private final String timeZone;
    private final String locale;

    public static class Parameters extends IConfigurationLoader.Parameters {
        /**
         * List of top level database schema elements with key {elementName} and value {typeName:required}.
         */
        public final Map<String, Pair<String, Boolean>> topLevelDatabaseElements = new LinkedHashMap<String, Pair<String, Boolean>>();
    }

    public ModuleSchemaLoader() {
        properties = new HashMap<String, String>();
        timeZone = null;
        locale = null;
    }

    public ModuleSchemaLoader(Map<String, String> properties, String timeZone, String locale) {
        Assert.notNull(properties);

        this.properties = properties;
        this.timeZone = timeZone;
        this.locale = locale;
    }

    public Set<ModuleSchemaConfiguration> loadModules(String path) {
        return (Set<ModuleSchemaConfiguration>) load(path, false);
    }

    public ModularDatabaseSchemaConfiguration loadInitialSchema(String path) {
        return (ModularDatabaseSchemaConfiguration) load(path, true);
    }

    private Object load(String path, boolean initialSchema) {
        Set<String> qualifiers;
        if (initialSchema)
            qualifiers = Collections.asSet("exadb.schema", "initial.exadb.schema");
        else
            qualifiers = Collections.asSet("exadb.schema");

        List<IConfigurationLoaderExtension> extensions = Services.loadProviders(IConfigurationLoaderExtension.class, qualifiers);
        Map<String, Pair<String, Boolean>> topLevelDatabaseElements = new LinkedHashMap<String, Pair<String, Boolean>>();
        StringBuilder builder = new StringBuilder();
        for (IConfigurationLoaderExtension extension : extensions) {
            IConfigurationLoader.Parameters p = extension.getParameters();
            if (!(p instanceof Parameters))
                continue;

            Parameters parameters = (Parameters) p;
            for (Map.Entry<String, Pair<String, Boolean>> entry : parameters.topLevelDatabaseElements.entrySet())
                builder.append(MessageFormat.format("{0}:'{ 'type=\"{1}\" required={2} '}'", entry.getKey(), entry.getValue().getKey(),
                        entry.getValue().getValue()));

            topLevelDatabaseElements.putAll(parameters.topLevelDatabaseElements);
        }

        Map<String, String> properties = new LinkedHashMap<String, String>(this.properties);
        properties.put("exa.extendedProperties", builder.toString());

        Parameters parameters = new Parameters();
        parameters.propertyResolvers.add(new MapPropertyResolver(properties));

        if (initialSchema)
            parameters.contextFactories.put(ModuleSchemaConfiguration.SCHEMA, new InitialSchemaLoadContext(topLevelDatabaseElements));
        else
            parameters.contextFactories.put(ModuleSchemaConfiguration.SCHEMA, new SchemaLoadContext(topLevelDatabaseElements));

        ConfigurationLoader loader = new ConfigurationLoader(parameters,
                new MapBuilder().put("timeZone", timeZone).put("locale", locale).toMap(), qualifiers, true);
        ILoadContext context = loader.loadConfiguration(path);
        return context.get(ModuleSchemaConfiguration.SCHEMA);
    }
}
