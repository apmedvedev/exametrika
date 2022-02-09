/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Locales;


/**
 * The {@link DatabaseSchema} is a database schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DatabaseSchema extends SchemaObject implements IDatabaseSchema {
    private final ModularDatabaseSchemaConfiguration configuration;
    private final long creationTime;
    private final int version;
    private final TimeZone timeZone;
    private final Locale locale;
    private final List<IDomainSchema> domains;
    private final Map<String, IDomainSchema> domainsMap;
    private final Map<String, IDomainSchema> domainsByAliasMap;
    private final Map<String, ISchemaObject> schemaObjects;

    public DatabaseSchema(ModularDatabaseSchemaConfiguration configuration, long creationTime, int version, List<IDomainSchema> domains,
                          boolean current) {
        super(TYPE);

        Assert.notNull(configuration);
        Assert.notNull(domains);

        this.configuration = configuration;
        this.creationTime = creationTime;
        this.version = version;
        this.timeZone = configuration.getTimeZone() != null ? TimeZone.getTimeZone(configuration.getTimeZone()) : TimeZone.getDefault();
        this.locale = configuration.getLocale() != null ? Locales.getLocale(configuration.getLocale()) : Locale.getDefault();
        this.domains = Immutables.wrap(domains);

        Map<String, ISchemaObject> schemaObjects = new LinkedHashMap<String, ISchemaObject>();
        Map<String, IDomainSchema> domainsMap = new HashMap<String, IDomainSchema>();
        Map<String, IDomainSchema> domainsByAliasMap = new HashMap<String, IDomainSchema>();
        for (IDomainSchema domain : domains) {
            domainsMap.put(domain.getConfiguration().getName(), domain);
            domainsByAliasMap.put(domain.getConfiguration().getAlias(), domain);
        }

        this.domainsMap = domainsMap;
        this.domainsByAliasMap = domainsByAliasMap;
        this.schemaObjects = schemaObjects;

        for (IDomainSchema domain : domains)
            ((DomainSchema) domain).setParent(this, schemaObjects);

        if (current) {
            for (IDomainSchema domain : domains)
                ((DomainSchema) domain).resolveDependencies();
        }

    }

    @Override
    public ModularDatabaseSchemaConfiguration getModularConfiguration() {
        return configuration;
    }

    @Override
    public DatabaseSchemaConfiguration getConfiguration() {
        return configuration.getCombinedSchema();
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public List<IDomainSchema> getDomains() {
        return domains;
    }

    @Override
    public IDomainSchema findDomain(String name) {
        Assert.notNull(name);

        return domainsMap.get(name);
    }

    @Override
    public IDomainSchema findDomainByAlias(String alias) {
        Assert.notNull(alias);

        return domainsByAliasMap.get(alias);
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        return (Iterable) domains;
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        Assert.notNull(type);

        if (type.equals(IDomainSchema.TYPE))
            return (Iterable) domains;
        else
            return Collections.emptyList();
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        Assert.notNull(type);
        Assert.notNull(name);

        if (type.equals(IDomainSchema.TYPE))
            return (T) domainsMap.get(name);
        else
            return null;
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        Assert.notNull(type);
        Assert.notNull(alias);

        if (type.equals(IDomainSchema.TYPE))
            return (T) domainsByAliasMap.get(alias);
        else
            return null;
    }

    @Override
    public <T extends ISchemaObject> T getParent() {
        return null;
    }

    @Override
    public <T extends ISchemaObject> T findSchemaById(String id) {
        Assert.notNull(id);

        if (id.equals(IDatabaseSchema.TYPE))
            return (T) this;

        return (T) schemaObjects.get(id);
    }
}
