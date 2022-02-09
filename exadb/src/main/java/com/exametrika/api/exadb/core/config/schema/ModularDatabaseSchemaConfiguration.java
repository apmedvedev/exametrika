/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Version;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link ModularDatabaseSchemaConfiguration} is a modular database schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ModularDatabaseSchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.initial-1.0";

    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<ModuleSchemaConfiguration> modules;
    private final Map<String, ModuleSchemaConfiguration> modulesMap;
    private final Map<String, ModuleSchemaConfiguration> modulesByAliasMap;
    private final String timeZone;
    private final String locale;
    private final DatabaseSchemaConfiguration combinedSchema;

    public ModularDatabaseSchemaConfiguration(String name, Set<ModuleSchemaConfiguration> modules) {
        this(name, name, null, modules, null, null);
    }

    public ModularDatabaseSchemaConfiguration(String name, String alias, String description, Set<ModuleSchemaConfiguration> modules,
                                              String timeZone, String locale) {
        super(name, alias, description);

        Assert.notNull(modules);

        Map<String, ModuleSchemaConfiguration> modulesMap = new HashMap<String, ModuleSchemaConfiguration>();
        Map<String, ModuleSchemaConfiguration> modulesByAliasMap = new HashMap<String, ModuleSchemaConfiguration>();
        for (ModuleSchemaConfiguration module : modules) {
            Assert.isNull(modulesMap.put(module.getName(), module));
            Assert.isNull(modulesByAliasMap.put(module.getAlias(), module));
            Assert.isTrue(module.getSchema().getTimeZone() == null);
            Assert.isTrue(module.getSchema().getLocale() == null);
        }

        for (ModuleSchemaConfiguration module : modules) {
            for (ModuleDependencySchemaConfiguration dependency : module.getDependencies()) {
                ModuleSchemaConfiguration dependencyModule = modulesMap.get(dependency.getName());
                if (dependencyModule == null)
                    throw new InvalidArgumentException(messages.moduleDependencyNotFound(module.getAlias(), module.getVersion(), dependency));
                if (!dependencyModule.getVersion().isCompatible(dependency.getVersion()))
                    throw new InvalidArgumentException(messages.moduleDependencyIsNotCompatible(module.getAlias(), module.getVersion(),
                            dependency, dependencyModule));
            }
        }

        List<ModuleSchemaConfiguration> modulesList = new ArrayList(modules);
        Collections.sort(modulesList, new Comparator<ModuleSchemaConfiguration>() {
            @Override
            public int compare(ModuleSchemaConfiguration o1, ModuleSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.modules = Immutables.wrap(modulesList);
        this.modulesMap = modulesMap;
        this.modulesByAliasMap = modulesByAliasMap;
        this.timeZone = timeZone;
        this.locale = locale;

        DatabaseSchemaConfiguration schema = new DatabaseSchemaConfiguration(getName(), getAlias(), getDescription(),
                Collections.<DomainSchemaConfiguration>emptySet(), Collections.<DatabaseExtensionSchemaConfiguration>emptySet(),
                timeZone, locale, true);
        for (ModuleSchemaConfiguration module : modules)
            schema = schema.combine(module.getSchema());
        this.combinedSchema = schema;
    }

    public List<ModuleSchemaConfiguration> getModules() {
        return modules;
    }

    public ModuleSchemaConfiguration findModule(String name) {
        Assert.notNull(name);

        return modulesMap.get(name);
    }

    public ModuleSchemaConfiguration findModuleByAlias(String alias) {
        Assert.notNull(alias);

        return modulesByAliasMap.get(alias);
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getLocale() {
        return locale;
    }

    public DatabaseSchemaConfiguration getCombinedSchema() {
        return combinedSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ModularDatabaseSchemaConfiguration))
            return false;

        ModularDatabaseSchemaConfiguration configuration = (ModularDatabaseSchemaConfiguration) o;
        return super.equals(configuration) && modules.equals(configuration.modules) && Objects.equals(timeZone, configuration.timeZone) &&
                Objects.equals(locale, configuration.locale);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(modules, timeZone, locale);
    }

    private interface IMessages {
        @DefaultMessage("Dependency ''{2}'' of module ''{0}-{1}'' is not found.")
        ILocalizedMessage moduleDependencyNotFound(String alias, Version version, ModuleDependencySchemaConfiguration dependency);

        @DefaultMessage("Dependency ''{2}'' of module ''{0}-{1}'' is not compatible with module ''{3}''.")
        ILocalizedMessage moduleDependencyIsNotCompatible(String alias, Version version,
                                                          ModuleDependencySchemaConfiguration dependency, ModuleSchemaConfiguration dependecyModule);
    }
}
