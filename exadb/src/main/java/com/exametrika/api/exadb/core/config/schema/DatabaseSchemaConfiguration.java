/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link DatabaseSchemaConfiguration} is a database schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseSchemaConfiguration extends SchemaConfiguration {
    private List<DomainSchemaConfiguration> domains;
    private final Map<String, DomainSchemaConfiguration> domainsMap;
    private final Map<String, DomainSchemaConfiguration> domainsByAliasMap;
    private List<DatabaseExtensionSchemaConfiguration> extensions;
    private final Map<String, DatabaseExtensionSchemaConfiguration> extensionsMap;
    private final Map<String, DatabaseExtensionSchemaConfiguration> extensionsByAliasMap;
    private final String timeZone;
    private final String locale;
    private boolean freezed;

    public DatabaseSchemaConfiguration(String name, Set<? extends DomainSchemaConfiguration> domains) {
        this(name, name, null, domains, Collections.<DatabaseExtensionSchemaConfiguration>emptySet());
    }

    public DatabaseSchemaConfiguration(String name) {
        this(name, name, null);
    }

    public DatabaseSchemaConfiguration(String name, String alias, String description) {
        this(name, alias, description, Collections.<DomainSchemaConfiguration>emptySet(),
                Collections.<DatabaseExtensionSchemaConfiguration>emptySet());
    }

    public DatabaseSchemaConfiguration(String name, String alias, String description, Set<? extends DomainSchemaConfiguration> domains,
                                       Set<? extends DatabaseExtensionSchemaConfiguration> extensions) {
        this(name, alias, description, domains, extensions, null, null, true);
    }

    public DatabaseSchemaConfiguration(String name, String alias, String description, Set<? extends DomainSchemaConfiguration> domains,
                                       Set<? extends DatabaseExtensionSchemaConfiguration> extensions, String timeZone, String locale, boolean freezed) {
        super(name, alias, description);

        Assert.notNull(domains);
        Assert.notNull(extensions);

        Map<String, DomainSchemaConfiguration> domainsMap = new HashMap<String, DomainSchemaConfiguration>();
        Map<String, DomainSchemaConfiguration> domainsByAliasMap = new HashMap<String, DomainSchemaConfiguration>();
        for (DomainSchemaConfiguration domain : domains) {
            Assert.isNull(domainsMap.put(domain.getName(), domain));
            Assert.isNull(domainsByAliasMap.put(domain.getAlias(), domain));
        }

        List<DomainSchemaConfiguration> domainsList = new ArrayList(domains);
        Collections.sort(domainsList, new Comparator<DomainSchemaConfiguration>() {
            @Override
            public int compare(DomainSchemaConfiguration o1, DomainSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        Map<String, DatabaseExtensionSchemaConfiguration> extensionsMap = new HashMap<String, DatabaseExtensionSchemaConfiguration>();
        Map<String, DatabaseExtensionSchemaConfiguration> extensionsByAliasMap = new HashMap<String, DatabaseExtensionSchemaConfiguration>();
        for (DatabaseExtensionSchemaConfiguration extension : extensions) {
            Assert.isNull(extensionsMap.put(extension.getName(), extension));
            Assert.isNull(extensionsByAliasMap.put(extension.getAlias(), extension));
        }

        List<DatabaseExtensionSchemaConfiguration> extensionsList = new ArrayList(extensions);
        Collections.sort(extensionsList, new Comparator<DatabaseExtensionSchemaConfiguration>() {
            @Override
            public int compare(DatabaseExtensionSchemaConfiguration o1, DatabaseExtensionSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.freezed = freezed;
        if (freezed)
            this.domains = Immutables.wrap(domainsList);
        else
            this.domains = domainsList;
        this.domainsMap = domainsMap;
        this.domainsByAliasMap = domainsByAliasMap;

        if (freezed)
            this.extensions = Immutables.wrap(extensionsList);
        else
            this.extensions = extensionsList;
        this.extensionsMap = extensionsMap;
        this.extensionsByAliasMap = extensionsByAliasMap;
        this.timeZone = timeZone;
        this.locale = locale;
    }

    public List<DomainSchemaConfiguration> getDomains() {
        return domains;
    }

    public DomainSchemaConfiguration findDomain(String name) {
        Assert.notNull(name);

        return domainsMap.get(name);
    }

    public DomainSchemaConfiguration findDomainByAlias(String alias) {
        Assert.notNull(alias);

        return domainsByAliasMap.get(alias);
    }

    public List<DatabaseExtensionSchemaConfiguration> getExtensions() {
        return extensions;
    }

    public DatabaseExtensionSchemaConfiguration findExtension(String name) {
        Assert.notNull(name);

        return extensionsMap.get(name);
    }

    public DatabaseExtensionSchemaConfiguration findExtensionByAlias(String alias) {
        Assert.notNull(alias);

        return extensionsByAliasMap.get(alias);
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getLocale() {
        return locale;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        Set<DomainSchemaConfiguration> domains = new HashSet<DomainSchemaConfiguration>();
        Map<String, DomainSchemaConfiguration> domainsMap = new HashMap<String, DomainSchemaConfiguration>(this.domainsMap);
        for (DomainSchemaConfiguration domain : ((DatabaseSchemaConfiguration) schema).getDomains())
            domains.add(combine(domain, domainsMap));
        domains.addAll(domainsMap.values());

        Set<DatabaseExtensionSchemaConfiguration> extensions = new HashSet<DatabaseExtensionSchemaConfiguration>();
        Map<String, DatabaseExtensionSchemaConfiguration> extensionsMap = new HashMap<String, DatabaseExtensionSchemaConfiguration>(this.extensionsMap);
        for (DatabaseExtensionSchemaConfiguration extension : ((DatabaseSchemaConfiguration) schema).getExtensions())
            extensions.add(combine(extension, extensionsMap));
        extensions.addAll(extensionsMap.values());

        return (T) new DatabaseSchemaConfiguration(getName(), getAlias(), getDescription(), domains, extensions,
                getTimeZone(), getLocale(), true);
    }

    public void addDomain(DomainSchemaConfiguration domain) {
        Assert.notNull(domain);
        Assert.checkState(!freezed);
        Assert.isTrue(findDomain(domain.getName()) == null);
        Assert.isTrue(findDomainByAlias(domain.getAlias()) == null);

        domains.add(domain);
        domainsMap.put(domain.getName(), domain);
        domainsByAliasMap.put(domain.getAlias(), domain);
    }

    public void addExtension(DatabaseExtensionSchemaConfiguration extension) {
        Assert.notNull(extension);
        Assert.checkState(!freezed);
        Assert.isTrue(findExtension(extension.getName()) == null);
        Assert.isTrue(findExtensionByAlias(extension.getAlias()) == null);

        extensions.add(extension);
        extensionsMap.put(extension.getName(), extension);
        extensionsByAliasMap.put(extension.getAlias(), extension);
    }

    public void add(DatabaseSchemaConfiguration schema) {
        Assert.notNull(schema);
        Assert.checkState(!freezed);

        for (DomainSchemaConfiguration domain : schema.getDomains())
            addDomain(domain);

        for (DatabaseExtensionSchemaConfiguration extension : schema.getExtensions())
            addExtension(extension);
    }

    public void freeze() {
        if (freezed)
            return;

        freezed = true;
        for (DomainSchemaConfiguration domain : domains)
            domain.freeze();
        domains = Immutables.wrap(domains);

        for (DatabaseExtensionSchemaConfiguration extension : extensions)
            extension.freeze();
        extensions = Immutables.wrap(extensions);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DatabaseSchemaConfiguration))
            return false;

        DatabaseSchemaConfiguration configuration = (DatabaseSchemaConfiguration) o;
        return super.equals(configuration) && domains.equals(configuration.domains) && extensions.equals(configuration.extensions) &&
                Objects.equals(timeZone, configuration.timeZone) && Objects.equals(locale, configuration.locale);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(domains, extensions, timeZone, locale);
    }
}
