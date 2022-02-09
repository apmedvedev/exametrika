/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;


/**
 * The {@link DomainSchema} is a domain schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DomainSchema extends SchemaObject implements IDomainSchema {
    private final DomainSchemaConfiguration configuration;
    private final long creationTime;
    private final int version;
    private final List<ISpaceSchema> spaces;
    private final Map<String, ISpaceSchema> spacesMap;
    private final Map<String, ISpaceSchema> spacesByAliasMap;
    private final List<IDomainServiceSchema> domainServices;
    private final Map<String, IDomainServiceSchema> domainServicesMap;
    private final Map<String, IDomainServiceSchema> domainServicesByAliasMap;
    private IDatabaseSchema parent;

    public DomainSchema(DomainSchemaConfiguration configuration, long creationTime, int version, List<ISpaceSchema> spaces,
                        List<IDomainServiceSchema> domainServices) {
        super(TYPE);

        Assert.notNull(configuration);
        Assert.notNull(spaces);
        Assert.notNull(domainServices);

        this.configuration = configuration;
        this.creationTime = creationTime;
        this.version = version;
        this.spaces = Immutables.wrap(spaces);
        this.domainServices = Immutables.wrap(domainServices);

        Map<String, ISpaceSchema> spacesMap = new HashMap<String, ISpaceSchema>();
        Map<String, ISpaceSchema> spacesByAliasMap = new HashMap<String, ISpaceSchema>();
        for (ISpaceSchema space : spaces) {
            spacesMap.put(space.getConfiguration().getName(), space);
            spacesByAliasMap.put(space.getConfiguration().getAlias(), space);
        }

        this.spacesMap = spacesMap;
        this.spacesByAliasMap = spacesByAliasMap;

        Map<String, IDomainServiceSchema> domainServicesMap = new HashMap<String, IDomainServiceSchema>();
        Map<String, IDomainServiceSchema> domainServicesByAliasMap = new HashMap<String, IDomainServiceSchema>();
        for (IDomainServiceSchema service : domainServices) {
            domainServicesMap.put(service.getConfiguration().getName(), service);
            domainServicesByAliasMap.put(service.getConfiguration().getAlias(), service);
        }

        this.domainServicesMap = domainServicesMap;
        this.domainServicesByAliasMap = domainServicesByAliasMap;
    }

    public void setParent(IDatabaseSchema database, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(database);

        this.parent = database;
        super.setParent(database, schemaObjects);

        for (ISpaceSchema space : spaces)
            ((ISpaceSchemaControl) space).setParent(this, schemaObjects);

        for (IDomainServiceSchema service : domainServices)
            ((DomainServiceSchema) service).setParent(this, schemaObjects);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        for (ISpaceSchema space : spaces)
            ((ISpaceSchemaControl) space).resolveDependencies();

        for (IDomainServiceSchema service : domainServices)
            ((DomainServiceSchema) service).resolveDependencies();
    }

    @Override
    public DomainSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public IDatabaseSchema getParent() {
        return parent;
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
    public List<ISpaceSchema> getSpaces() {
        return spaces;
    }

    @Override
    public <T extends ISpaceSchema> T findSpace(String name) {
        Assert.notNull(name);

        return (T) spacesMap.get(name);
    }

    @Override
    public <T extends ISpaceSchema> T findSpaceByAlias(String alias) {
        Assert.notNull(alias);

        return (T) spacesByAliasMap.get(alias);
    }

    @Override
    public List<IDomainServiceSchema> getDomainServices() {
        return domainServices;
    }

    @Override
    public <T extends IDomainServiceSchema> T findDomainService(String name) {
        Assert.notNull(name);

        return (T) domainServicesMap.get(name);
    }

    @Override
    public <T extends IDomainServiceSchema> T findDomainServiceByAlias(String alias) {
        Assert.notNull(alias);

        return (T) domainServicesByAliasMap.get(alias);
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        List<ISchemaObject> children = new ArrayList<ISchemaObject>();
        children.addAll(spaces);
        children.addAll(domainServices);
        return children;
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        Assert.notNull(type);

        if (type.equals(ISpaceSchema.TYPE))
            return (Iterable) spaces;
        else if (type.equals(IDomainServiceSchema.TYPE))
            return (Iterable) domainServices;
        else
            return Collections.emptyList();
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        Assert.notNull(type);
        Assert.notNull(name);

        if (type.equals(ISpaceSchema.TYPE))
            return (T) spacesMap.get(name);
        else if (type.equals(IDomainServiceSchema.TYPE))
            return (T) domainServicesMap.get(name);
        else
            return null;
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        Assert.notNull(type);
        Assert.notNull(alias);

        if (type.equals(ISpaceSchema.TYPE))
            return (T) spacesByAliasMap.get(alias);
        else if (type.equals(IDomainServiceSchema.TYPE))
            return (T) domainServicesByAliasMap.get(alias);
        else
            return null;
    }
}
