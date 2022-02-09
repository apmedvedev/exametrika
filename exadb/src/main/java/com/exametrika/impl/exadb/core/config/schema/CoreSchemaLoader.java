/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.BackupOperationSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.FileArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleDependencySchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.NullArchiveStoreSchemaConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Version;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link CoreSchemaLoader} is a configuration loader for module schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CoreSchemaLoader extends AbstractElementLoader implements IExtensionLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        SchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);

        for (Map.Entry<String, Object> entry : element)
            loadContext.addModule(loadModule(entry.getKey(), (JsonObject) entry.getValue(), loadContext, context));
    }

    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("BackupOperation")) {
            ArchiveStoreSchemaConfiguration archiveStore = loadArchiveStore((JsonObject) element.get("archiveStore"), context);

            return new BackupOperationSchemaConfiguration(archiveStore);
        } else if (type.equals("FileArchiveStore") || type.equals("NullArchiveStore"))
            return loadArchiveStore(element, context);
        else if (type.equals("Modules")) {
            loadElement(element, context);
            return null;
        } else
            throw new InvalidConfigurationException();
    }

    private ArchiveStoreSchemaConfiguration loadArchiveStore(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("FileArchiveStore")) {
            String path = element.get("path");
            return new FileArchiveStoreSchemaConfiguration(path);
        } else if (type.equals("NullArchiveStore"))
            return new NullArchiveStoreSchemaConfiguration();
        else
            return load(null, type, element, context);
    }

    private ModuleSchemaConfiguration loadModule(String name, JsonObject element, SchemaLoadContext loadContext,
                                                 ILoadContext context) {
        String alias = element.get("alias", name);
        String description = element.get("description", null);
        Version version = loadVersion(element.get("version"));
        DatabaseSchemaConfiguration schema = loadSchemaHeader(name, (JsonObject) element.get("schema"), context);

        JsonObject dependenciesElement = element.get("dependencies");
        Set<ModuleDependencySchemaConfiguration> dependencies = new LinkedHashSet<ModuleDependencySchemaConfiguration>();
        for (Map.Entry<String, Object> entry : dependenciesElement)
            dependencies.add(loadDependency(entry.getKey(), (JsonObject) entry.getValue()));

        ModuleSchemaConfiguration module = new ModuleSchemaConfiguration(name, alias, description, version, schema, dependencies);
        loadContext.setCurrentModule(module);
        loadSchema(schema, (JsonObject) element.get("schema"), loadContext, context);

        return module;
    }

    private Version loadVersion(Object object) {
        if (object instanceof JsonObject) {
            JsonObject element = (JsonObject) object;
            long major = element.get("major");
            long minor = element.get("minor");
            long patch = element.get("patch");
            String preRelease = element.get("preRelease", null);
            String buildMetadata = element.get("buildMetadata", null);
            return new Version((int) major, (int) minor, (int) patch, preRelease, buildMetadata);
        } else
            return Version.parse((String) object);
    }

    private DatabaseSchemaConfiguration loadSchemaHeader(String name, JsonObject element, ILoadContext context) {
        String alias = element.get("alias", name);
        String description = element.get("description", null);

        Set<DomainSchemaConfiguration> domains = new LinkedHashSet<DomainSchemaConfiguration>();
        Set<DatabaseExtensionSchemaConfiguration> extensions = new LinkedHashSet<DatabaseExtensionSchemaConfiguration>();

        return new DatabaseSchemaConfiguration(name, alias, description, domains, extensions, null, null, false);
    }

    private void loadSchema(DatabaseSchemaConfiguration schema, JsonObject element, SchemaLoadContext loadContext, ILoadContext context) {
        String type = getType(element);
        if (type.equals("Database")) {
            JsonObject domainsElement = element.get("domains");
            for (Map.Entry<String, Object> entry : domainsElement)
                schema.addDomain(loadDomain(entry.getKey(), (JsonObject) entry.getValue(), context));

            JsonArray extensionsElement = element.get("extensions");
            for (Object e : extensionsElement)
                schema.addExtension((DatabaseExtensionSchemaConfiguration) load(null, null, (JsonObject) e, context));

            for (Map.Entry<String, Object> entry : element) {
                if (entry.getKey().equals("alias") || entry.getKey().equals("description") || entry.getKey().equals("domains") ||
                        entry.getKey().equals("extensions") || entry.getKey().equals("instanceOf"))
                    continue;

                Pair<String, Boolean> pair = loadContext.getTopLevelDatabaseElements().get(entry.getKey());
                Assert.notNull(pair);
                String topLevelElementType = pair.getKey();
                load(entry.getKey(), topLevelElementType, entry.getValue(), context);
            }
        } else
            load(null, type, element, context);
    }

    private ModuleDependencySchemaConfiguration loadDependency(String name, JsonObject element) {
        Version version = loadVersion(element.get("version"));
        return new ModuleDependencySchemaConfiguration(name, version);
    }

    private DomainSchemaConfiguration loadDomain(String name, JsonObject element, ILoadContext context) {
        String alias = element.get("alias", name);
        String description = element.get("description", null);

        JsonObject spacesElement = element.get("spaces");
        Set<SpaceSchemaConfiguration> spaces = new LinkedHashSet<SpaceSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : spacesElement)
            spaces.add((SpaceSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

        JsonArray domainServicesElement = element.get("domainServices");
        Set<DomainServiceSchemaConfiguration> domainServices = new LinkedHashSet<DomainServiceSchemaConfiguration>();
        for (Object e : domainServicesElement)
            domainServices.add((DomainServiceSchemaConfiguration) load(null, null, (JsonObject) e, context));

        return new DomainSchemaConfiguration(name, alias, description, spaces, domainServices, false);
    }
}
